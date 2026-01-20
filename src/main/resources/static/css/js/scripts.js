window.addEventListener('DOMContentLoaded', event => {
    // Tìm cái nút có ID là sidebarToggle
    const sidebarToggle = document.body.querySelector('#sidebarToggle');

    if (sidebarToggle) {
        // Nếu click vào nút đó -> Thêm/Xóa class 'sb-sidenav-toggled' cho body
        sidebarToggle.addEventListener('click', event => {
            event.preventDefault();
            document.body.classList.toggle('sb-sidenav-toggled');
        });
    }
});