<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org" xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head>
    <meta charset="UTF-8">
    <title>Home</title>
</head>
<body>
<img src="../../images/shopping.png" th:src="@{/images/shopping.png}"/>
<h1 th:text="${message}">mess</h1>
<h2 th:text="#{home.welcome}">welcome</h2>
<p th:text="${date}"></p>

<p>Please select an option</p>
<ol> [cite: 295]
    <li><a href="product/list.html" th:href="@{/product}">Product List</a></li>

    <li sec:authorize="hasAnyRole('CUSTOMER', 'ADMIN')">
        <a href="order/list.html" th:href="@{/order}">Order List</a>
    </li> [cite: 296]

    <li sec:authorize="!isAuthenticated()">
        <a th:href="@{/login}">Login</a>
    </li>
    <li sec:authorize="isAuthenticated()">
        <a th:href="@{/logout}">Logout</a>
    </li>
</ol>
</body>
</html>