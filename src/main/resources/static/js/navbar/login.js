htmx.on("htmx:afterRequest", () => {
    if(event.detail.target.id.includes("login") && event.detail.xhr.status === 200) {
        document.getElementById("login_error").innerHTML = "";
        setTimeout(() => {
            bootstrap.Modal.getInstance(document.getElementById("login_modal")).hide();
            document.getElementById("login_form").reset();
            document.getElementById("login_response").innerHTML = "";
            htmx.ajax("GET", "/navbar", {
                target: "#navbar",
                swap: "innerHTML"
            });
        }, 1000);
    }
});