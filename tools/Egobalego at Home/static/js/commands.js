var lastId;

document.addEventListener("DOMContentLoaded", async function () {
    const responseForLastId = await fetch("/last_id");
    lastId = parseInt(await responseForLastId.text());
    const responseForServerData = await fetch("/server_data");
    var serverData = await responseForServerData.json();
    var command_types = ["command", "operation"]
    serverData.forEach(item => {
        if (command_types.includes(item.type))
            addCommandCard(false, item);
    });
});

async function updateServer(card, action) {
    var id = card.querySelector("#card-id").value;
    var type = card.querySelector("#command-type").value;
    var content = card.querySelector("#command-content").value;
    var x = card.querySelector("#x-coord").value;
    var y = card.querySelector("#y-coord").value;
    var z = card.querySelector("#z-coord").value;
    var active = card.querySelector("#card-enabler-switch").checked;

    var commandData = {
        "id": id,
        "type": type === "manual" ? "command" : "operation",
        "active": active
    };
    if (type === "manual")
        commandData["content"] = content;
    else {
        commandData["name"] = type;
        if (type !== "rmResearcher") {
            commandData["x"] = parseInt(x) || 0;
            commandData["y"] = parseInt(y) || 0;
            commandData["z"] = parseInt(z) || 0;
        }
    }
    commandData = { [action]: [commandData] };

    var xhr = new XMLHttpRequest();
    xhr.open("POST", "/data_receiver");
    xhr.setRequestHeader("Content-Type", "application/json");
    xhr.send(JSON.stringify(commandData));
}

document.getElementById("add-card-button").addEventListener("click", function () {
    addCommandCard(true);
});

function addCommandCard(isNew, item) {
    // Add template to top of container
    var template = document.querySelector("#command-template").content.cloneNode(true);
    var cardContainer = document.getElementById("card-container");
    cardContainer.insertBefore(template, cardContainer.firstChild);

    // Get template elements
    var thisCard = document.getElementById("command-card");

    var id = thisCard.querySelector("#card-id");

    var warningDiv = thisCard.querySelector("#no-type-warning");

    var cardEnablerDiv = thisCard.querySelector("#card-enabler");
    var cardEnablerSwitch = cardEnablerDiv.querySelector("#card-enabler-switch");
    var cardEnablerLabel = cardEnablerDiv.querySelector("#card-enabler-label");

    var deleteCardButton = thisCard.querySelector("#delete-card-button");

    var confirmDeletionModal = document.getElementById("modal-confirm-deletion");
    var deleteButtonModal = document.getElementById("delete-button-modal");

    var commandSelect = thisCard.querySelector("#command-type");

    var manualCommandDiv = thisCard.querySelector("#manual-command");
    var commandContent = manualCommandDiv.querySelector("#command-content");

    var coordinatesDiv = thisCard.querySelector("#coordinates");
    var x = coordinatesDiv.querySelector("#x-coord");
    var y = coordinatesDiv.querySelector("#y-coord");
    var z = coordinatesDiv.querySelector("#z-coord");

    if (isNew) {
        id.value = "command-" + (lastId + 1);
        lastId++;
    }
    else {
        id.value = item.id;
        switch (item.type) {
            case "command":
                commandSelect.value = "manual";
                manualCommandDiv.hidden = false;
                commandContent.value = item.content;
                break;
            case "operation":
                commandSelect.value = item.name;
                if (item.name !== "rmResearcher") {
                    coordinatesDiv.hidden = false;
                    x.value = item.x;
                    y.value = item.y;
                    z.value = item.z;
                }
                break;
        }
        warningDiv.hidden = true;
        cardEnablerDiv.hidden = false;
        cardEnablerSwitch.checked = item.active
        changeColorAndLabel(item.active);
    }

    deleteCardButton.addEventListener("click", function () {
        if (commandSelect.value === "none") {
            thisCard.remove();
        } else {
            const modal = new bootstrap.Modal(confirmDeletionModal);
            modal.show();
            deleteButtonModal.addEventListener("click", function () {
                updateServer(thisCard, "remove");
                thisCard.remove();
            });
        }
    });

    cardEnablerSwitch.addEventListener("click", function () {
        changeColorAndLabel(this.checked);
    });

    commandSelect.addEventListener("change", function () {
        var selectedCommand = commandSelect.value;
        if (selectedCommand !== "none") {
            warningDiv.hidden = true;
            cardEnablerDiv.hidden = false;
            cardEnablerSwitch.checked = false;
            changeColorAndLabel(false);
            switch (selectedCommand) {
                case "manual":
                    showElements(manualCommandDiv);
                    hideElements(coordinatesDiv);
                    break;
                case "rmResearcher":
                    hideElements(manualCommandDiv, coordinatesDiv);
                    break;
                case "tpResearcher":
                case "spawnResearcher":
                case "rmTent":
                    showElements(coordinatesDiv);
                    hideElements(manualCommandDiv);
                    break;
            }
        } else {
            warningDiv.hidden = false;
            cardEnablerDiv.hidden = true;
            thisCard.classList.remove("bg-danger-subtle", "bg-success-subtle");
            thisCard.classList.add("bg-dark-subtle");
            hideElements(manualCommandDiv, coordinatesDiv);
        }
    });

    cardContainer.querySelectorAll('input, select, textarea').forEach(function (element) {
        element.addEventListener('change', function () {
            updateServer(thisCard, commandSelect.value !== "none" ? "add" : "remove");
        });
    });

    function changeColorAndLabel(isActive) {
        if (isActive)
            thisCard.classList.remove("bg-dark-subtle", "bg-danger-subtle");
        else
            thisCard.classList.remove("bg-dark-subtle", "bg-success-subtle");
        thisCard.classList.add(isActive ? "bg-success-subtle" : "bg-danger-subtle");
        cardEnablerLabel.innerText = isActive ? "Attivo" : "Non attivo";
    }

    function showElements(...divs) {
        for (var div of divs) {
            div.hidden = false;
        }
    }

    function hideElements(...divs) {
        for (var div of divs) {
            div.hidden = true;
        }
    }
}