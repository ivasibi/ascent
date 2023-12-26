htmx.on("htmx:afterRequest", () => {
    if(event.detail.target.id.includes("register") && event.detail.xhr.status === 201) {
        document.getElementById("register_error").innerHTML = "";
        setTimeout(() => {
            bootstrap.Modal.getInstance(document.getElementById("register_modal")).hide();
            document.getElementById("register_form").reset();
            document.getElementById("register_response").innerHTML = "";
        }, 1000);
    }
});