<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Поиск</title>
    <jsp:include page="head.jsp"/>
</head>
<body>
<jsp:include page="footer.jsp"/>

<div class="container">
    <form:form method="GET" action="search">
    <div class="row">
        <div class="col-lg-7">
            <div class="input-group">

                <input type="text" name="searchQuery" class="form-control" placeholder="Введите библиотечный номер; название документа, кафедры; фамилию автора">
            <span class="input-group-btn">
              <button class="btn btn-default" type="submit">Поиск</button>
            </span>

            </div><!-- /input-group -->
        </div><!-- /.col-lg-6 -->
    </div><!-- /.row -->
    </form:form>

</div>
</body>
</html>

