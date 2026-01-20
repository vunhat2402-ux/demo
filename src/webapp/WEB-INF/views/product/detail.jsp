<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head><meta charset="UTF-8"><title>Product Detail</title></head>
<body>
<h1>Product Detail</h1>
<c:if test="${not empty product}">
    <p>Name: ${product.name}</p>
    <p>Price: ${product.price}</p>
    <h3>Comments:</h3>
    <c:if test="${empty product.comments}">
        <p><em>Nothing</em></p>
    </c:if>
    <c:forEach var="c" items="${product.comments}">
        <div style="border-left:3px solid #2a7; padding-left:8px; margin:6px 0;">
                ${c.text}
        </div>
    </c:forEach>
</c:if>
</body>
</html>
