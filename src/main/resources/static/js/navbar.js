htmx.on("htmx:load", () => {
    if(document.getElementById("login_modal")) {
        document.getElementById("login_modal").addEventListener("shown.bs.modal", () => {
            document.getElementById("login_form")[0].focus();
        });
    }

    if(document.getElementById("logout_modal")) {
        document.getElementById("logout_modal").addEventListener("shown.bs.modal", () => {
            document.getElementById("logout_form")[0].focus();
        });
    }

    if(document.getElementById("register_modal")) {
        document.getElementById("register_modal").addEventListener("shown.bs.modal", () => {
            document.getElementById("register_form")[0].focus();
        });
    }
});