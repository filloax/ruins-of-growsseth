var socket = io();

document.addEventListener("DOMContentLoaded", function () {
    let footer = document.querySelector('footer');
    let main = document.querySelector('main');
    main.style.paddingBottom = footer.offsetHeight + 'px';

    let noWebsocketWarning = document.getElementById("no-websocket");

    let modResponse = document.getElementById("mod-response");
    let modResponseStatus = document.getElementById("mod-response-status");
    let modResponseTimestamp = document.getElementById("mod-response-timestamp");
    let modResponseDetails = document.getElementById("mod-response-details");

    let reloadButtonDiv = document.getElementById("reload-button-div");
    let reloadButton = reloadButtonDiv.querySelector("#reload-button");

    let cardContainer = document.getElementById("card-container");
    let allCardButtons = cardContainer.querySelectorAll("button");

    let dialogueCard = document.getElementById("websocket-dialogue-card");
    let toastCard = document.getElementById("websocket-toast-card");
    let commandCard = document.getElementById("websocket-command-card");

    let sendDialogueButton = dialogueCard.querySelector("#send-dialogue-button");
    let dialogueContent = dialogueCard.querySelector("#dialogue-content");

    let sendToastButton = toastCard.querySelector("#send-toast-button");
    let toastType = toastCard.querySelector("#toast-type");
    let toastIconDiv = toastCard.querySelector("#toast-icon");
    let toastTitle = toastCard.querySelector("#toast-title");
    let toastIconNamespace = toastCard.querySelector("#namespace");
    let toastIconItemId = toastCard.querySelector("#item-id");
    let toastContent = toastCard.querySelector("#toast-content");

    let sendCommandButton = commandCard.querySelector("#send-command-button");
    let commandContent = commandCard.querySelector("#command-content");


    socket.emit("is_mod_connected", (response) => {
        if (response === true)
            enableConsole()
        else
            disableConsole()
    });


    reloadButton.addEventListener("click", function () {
        socket.emit("reload");
    });

    toastType.onchange = function () {
        toastIconDiv.hidden = (toastType.value == "toast-simple");
    };


    sendDialogueButton.addEventListener("click", function () {
        let dialogue = { "content": dialogueContent.value }
        socket.emit("rdialogue", dialogue);
    });

    sendToastButton.addEventListener("click", function () {
        let toast = { "title": toastTitle.value }
        if (toastType.value == "toast-with-icon")
            toast["item"] = toastIconNamespace.value + ":" + toastIconItemId.value
        if (toastContent.value !== "")
            toast["message"] = toastContent.value
        socket.emit("toast", toast);
    });

    sendCommandButton.addEventListener("click", function () {
        let command = { "command": commandContent.value }
        socket.emit("cmd", command);
    });


    socket.on('mod_connect', function() {
        enableConsole()
    });

    socket.on('mod_disconnect', function() {
        disableConsole()
    });


    socket.on('mod_response', (responseString) => {
        let response = JSON.parse(responseString);
        modResponseStatus.value = response.status;
        if (response.status === "success")
            modResponseStatus.style.color = "green";
        else
            modResponseStatus.style.color = "red";
        modResponseTimestamp.value = getCurrentTime();
        delete response.status
        modResponseDetails.value = JSON.stringify(response)
    });


    function enableConsole() {
        allCardButtons.forEach(button => button.disabled = false);
        reloadButton.disabled = false;
        noWebsocketWarning.hidden = true;
        modResponse.hidden = false;
    }

    function disableConsole() {
        allCardButtons.forEach(button => button.disabled = true);
        reloadButton.disabled = true;
        noWebsocketWarning.hidden = false;
        modResponse.hidden = true;
    }
});


function getCurrentTime() {
    let now = new Date();
    return new String(now).slice(16, 24);   // only getting the timestamp
}