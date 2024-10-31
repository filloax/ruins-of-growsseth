var socket = io();

document.getElementById("reload-button").addEventListener("click", function () {
    socket.emit("reload");
});

document.addEventListener("DOMContentLoaded", function () {
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
});