<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>

<!DOCTYPE html>
<html>
<head>
    <title>Мои документы</title>
    <jsp:include page="../public/service/head.jsp"/>

</head>

<body>

<jsp:include page="../public/service/header.jsp"/>



<div class="container">
    <h3>Мои документы</h3>
    <table class="table table-striped table-hover">
        <thead>
        <tr>
            <th>Библ. №</th>
            <th>Название документа</th>
            <th>Авторы</th>
            <th>Год издания</th>
            <th>Кафедра</th>
            <th></th>
            <th></th>
        </tr>
        </thead>
        <tbody>
        <c:forEach items="${documentList}" var="document">
            <tr>
                <td>${document.libraryKey}</td>
                <td>${document.name}</td>
                <td>
                    <c:forEach items="${document.authorSet}" var="author">
                        ${author.surname} ${author.name}${author.patronymic}
                    </c:forEach>
                </td>
                <td>${document.publishingYear}</td>
                <td>${document.department.name}</td>
                <td><a class="btn btn-default btn-xs" target="_blank" href="https://drive.google.com/open?id=${document.objectKey}">Просмотреть</a></td>
                <td>
                    <form:form method="POST" action="deny">
                        <input type="hidden" name="denyId" value="${document.id}"/>
                        <input type="submit" value="Удалить" class="btn btn-danger btn-xs"/>
                    </form:form>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</div>
</body>
</html>