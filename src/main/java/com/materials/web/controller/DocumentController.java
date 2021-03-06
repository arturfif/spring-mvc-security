package com.materials.web.controller;

import com.materials.web.dao.inter.*;
import com.materials.web.dto.DocumentDto;
import com.materials.web.model.*;
import com.materials.web.model.enumeration.Status;
import com.materials.web.util.MultipartFileUtil;
import com.materials.web.util.XMLMaker;
import com.materials.web.util.google.GoogleDriveUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping(value = "document/")
public class DocumentController {

    private UserDAO userDAO;
    private SpecialtyDAO specialtyDAO;
    private DepartmentDAO departmentDAO;
    private DocumentDAO documentDAO;
    private AuthorDAO authorDAO;
    private List<Faculty> facultyList;
    private List<Specialty> specialtyList;
    private List<Department> departmentList;

    @Autowired
    public DocumentController(UserDAO userDAO, SpecialtyDAO specialtyDAO, FacultyDAO facultyDAO,
                              DepartmentDAO departmentDAO, DocumentDAO documentDAO, AuthorDAO authorDAO) {
        this.userDAO = userDAO;
        this.specialtyDAO = specialtyDAO;
        this.departmentDAO = departmentDAO;
        this.documentDAO = documentDAO;
        this.authorDAO = authorDAO;
        facultyList = facultyDAO.list();
        specialtyList = specialtyDAO.list();
        departmentList = departmentDAO.list();
    }

    @RequestMapping(value = "add", method = RequestMethod.GET)
    public String addFile(Model model) {
        model.addAttribute("documentDto", new DocumentDto());
        model.addAttribute("facultyList", facultyList);
        model.addAttribute("specialtyList", specialtyList);
        model.addAttribute("departmentList", departmentList);
        return "admin/add-file";
    }

    @RequestMapping(value = "add", method = RequestMethod.POST)
    public String saveFile(@Valid @ModelAttribute("documentDto") DocumentDto documentDto,
                           BindingResult bindingResult, Model model,
                           RedirectAttributes redirectAttributes,
                           HttpServletRequest request) {


        boolean fail = false;
        if (bindingResult.hasErrors()) {
            fail = true;
        }
        if(request.isUserInRole("ADMIN")) {
            documentDto.setStatus(Status.CHECKED.name());
        } else {
            documentDto.setStatus(Status.UNCHECKED.name());
        }

        String username = request.getUserPrincipal().getName();
        documentDto.setUser(userDAO.get(username));

        try {
            saveDocumentAndUploadFile(documentDto);
        }
        catch (Exception e) {

            redirectAttributes.addFlashAttribute("error", "Не удалось загрузить файл!");
            return "redirect:add";
        }


        if(fail) {
            redirectAttributes.addFlashAttribute("error", "При загрузке файла произошла ошибка!");
            return "redirect:add";
        }

        redirectAttributes.addFlashAttribute("success", "Документ успешно добавлен в систему!");
        return "redirect:add";
    }

    private void saveDocumentAndUploadFile(@Valid @ModelAttribute("documentDto") DocumentDto documentDto) throws IOException, JAXBException {
        File file = MultipartFileUtil.multipartToFile(documentDto.getFile(), documentDto.getName());
        String objectKey = GoogleDriveUtil.uploadFile(file);
        documentDto.setObjectKey(objectKey);

        Document document = documentDto.buildDocument();
        document.setSpecialtySet(toSpecialtySet(documentDto.getSpecialtySet()));
        document.setAuthorSet(toAuthorSet(documentDto.getAuthorSet()));
        document.setDepartment(toDepartment(documentDto.getDepartmentId()));
        documentDAO.save(document);

        updateXMLFile();
    }

    private void updateXMLFile() throws JAXBException, IOException {
        File xmlFile = new File(System.getProperty("user.home") + "\\methodical-materials.xml");
        XMLMaker.buildXMLFile(documentDAO.listChecked(), xmlFile);
        GoogleDriveUtil.updateXMLFile(xmlFile);
    }

    @RequestMapping(value = "my", method = RequestMethod.GET)
    public String myDocuments(Model model, HttpServletRequest request) {
        String username = request.getUserPrincipal().getName();
        List<Document> documentList = new ArrayList<>();
        documentList.addAll(userDAO.get(username).getDocumentSet());
        model.addAttribute("documentList", documentList);
        return "admin/my-document";
    }

    @RequestMapping(value = "check", method = RequestMethod.GET)
    public String checkDocuments(Model model) {
        model.addAttribute("documentList", documentDAO.listUnchecked());
        return "admin/check-document";
    }

    @RequestMapping(value = "accept", method = RequestMethod.POST)
    public String acceptDocument(@RequestParam(value = "acceptId") long acceptId,
                                 RedirectAttributes redirectAttributes) {
        try {
            documentDAO.setCheckedStatus(acceptId);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Не удалось принять документ!");
        }
        redirectAttributes.addFlashAttribute("success", "Документ успешно принят и доступен для поиска!");
        return "redirect:check";
    }

    @RequestMapping(value = "deny", method = RequestMethod.POST)
    public String denyDocument(@RequestParam(value = "denyId") long denyId,
                               RedirectAttributes redirectAttributes) {
        try {
            GoogleDriveUtil.deleteFile(documentDAO.get(denyId).getObjectKey());
            documentDAO.remove(denyId);
            updateXMLFile();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Не удалось отклонить документ!");
            return "redirect:check";
        }
        redirectAttributes.addFlashAttribute("success", "Документ успешно отклонен!");

        return "redirect:check";
    }

    private Set<Specialty> toSpecialtySet(Set<Long> longSet) {
        return longSet.stream().map(aLong -> specialtyDAO.get(aLong)).collect(Collectors.toSet());
    }

    private Department toDepartment(Long id) {
        return departmentDAO.get(id);
    }

    private Set<Author> toAuthorSet(Set<String> stringSet) {
        Set<Author> authorSet = new HashSet<>();
        for (String string : stringSet) {
            authorSet.add(toAuthor(string));
        }
        return authorSet;
    }


    private Author toAuthor(String string) {

        Author author = new Author();

        String trim = string.trim();
        int spaceIndex = trim.indexOf(" ");
        String surname = trim.substring(0, spaceIndex);
        String name = trim.substring(spaceIndex + 1, spaceIndex + 3);
        String patronymic;

        if(trim.length() > spaceIndex + 4) {
            patronymic = trim.substring(spaceIndex + 3, spaceIndex + 5);

        } else {
            patronymic = "";
        }

        author.setSurname(surname);
        author.setName(String.valueOf(name));
        author.setPatronymic(patronymic);

        Author authorFromDB = authorDAO.get(author);

        if(authorFromDB != null) {
            return authorFromDB;
        }
        return author;
    }



}
