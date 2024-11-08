// Does not run on websocket page, but in every other

var socket = io();

document.addEventListener("DOMContentLoaded", function () {
    let footer = document.querySelector('footer');
    let main = document.querySelector('main');
    let reloadButtonDiv = document.getElementById("reload-button-div");
    let reloadButton = document.getElementById("reload-button");

    reloadButton.addEventListener("click", function () {
        socket.emit("reload");
    });

    socket.emit("is_mod_connected", (response) => {
        if (response === true)
            showFooter();
        else
            hideFooter();
    });

    socket.on('mod_connect', function () {
        showFooter();
    });

    socket.on('mod_disconnect', function () {
        hideFooter();
    });

    socket.on('mod_response', (responseString) => {
        let response = JSON.parse(responseString);
        if (response.status === "success")
            reportResponse(true)
        else
            reportResponse(false)
    });

    function showFooter() {
        reloadButtonDiv.hidden = false;
        main.style.paddingBottom = footer.offsetHeight + 'px';
    }

    function hideFooter() {
        reloadButtonDiv.hidden = true;
        main.style.paddingBottom = 0;
    }

    async function reportResponse(isSuccess) {
        reloadButton.classList.remove("btn-primary");
        reloadButton.classList.add(isSuccess ? "btn-success" : "btn-danger");
        setTimeout(function () {
            reloadButton.classList.remove(isSuccess ? "btn-success" : "btn-danger");
            reloadButton.classList.add("btn-primary");
        }, 1000);
    }
});