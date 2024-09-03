function registerAfterRequest() {
    if(event.detail.xhr.status === 201) {
        document.getElementById("register_error").innerHTML = "";
        setTimeout(() => {
            bootstrap.Modal.getInstance(document.getElementById("register_modal")).hide();
            document.getElementById("register_form").reset();
            document.getElementById("register_response").innerHTML = "";
        }, 1000);
    }
}