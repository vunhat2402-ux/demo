<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head>
</head>
<body>
<h1>List Product</h1>

<div sec:authorize="hasRole('ADMIN')" style="margin-bottom: 20px;">
    <a th:href="@{/product/add}" class="btn btn-primary">
        Add New Product
    </a>
</div>

<div th:each="product: ${products}" class="product">
    <a href="product/productdetail.html" th:href="@{'/product/' + ${product.id}}">
        <h2 th:text="${product.name}">Product Name</h2> [cite: 479]
    </a>
    <p><strong>Price:</strong> <span th:text="${product.price}">0</span> USD</p>

    <div sec:authorize="hasRole('ADMIN')">
        <a th:href="@{'/product/edit/' + ${product.id}}">Edit</a>
    </div>

    <div class="comments">
        <h3>Comments:</h3>
    </div>
</div>
</body>
</html>