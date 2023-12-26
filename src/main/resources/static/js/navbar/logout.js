htmx.on("htmx:afterRequest", () => {
    if(event.detail.target.id.includes("logout") && event.detail.xhr.status === 200) {
        document.getElementById("logout_error").innerHTML = "";
        setTimeout(() => {
            bootstrap.Modal.getInstance(document.getElementById("logout_modal")).hide();
            document.getElementById("logout_response").innerHTML = "";
            htmx.ajax("GET", "/navbar", {
                target: "#navbar",
                swap: "innerHTML"
            });
        }, 1000);
    }
});