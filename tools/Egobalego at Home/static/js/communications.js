var lastId;

document.addEventListener("DOMContentLoaded", async function () {
    const responseForLastId = await fetch("/last_id");
    lastId = parseInt(await responseForLastId.text());
    const responseForServerData = await fetch("/server_data");
    var serverData = await responseForServerData.json();
    var communication_types = ["dialogue", "toast", "researcherDiary", "structureBook"]
    serverData.forEach(item => {
        if (communication_types.includes(item.type))
            addCommCard(false, item);
    });
});

async function updateServer(card, action) {
    var id = card.querySelector("#card-id").value
    var typeSpecific = card.querySelector("#communication-type").value;
    var diaryStructure = card.querySelector("#diary-structure").value;
    var bookStructure = card.querySelector("#book-structure").value;
    var iconNamespace = card.querySelector("#namespace").value;
    var iconItemId = card.querySelector("#item-id").value;
    var title = card.querySelector("#title-text").value;
    var content = card.querySelector("#content-text").value;
    var active = card.querySelector("#card-enabler-switch").checked;

    var commData = {
        "id": id,
        "type": "",
        "content": content,
        "active": active
    };
    switch (typeSpecific) {
        case "dialogue":
            commData["type"] = typeSpecific
            break;
        case "toast-simple":
        case "toast-with-icon":
            commData["type"] = "toast"
            if (typeSpecific === "toast-with-icon")
                commData["icon"] = iconNamespace + ":" + iconItemId
            commData["title"] = title
            break;
        case "researcher-diary-new":
        case "researcher-diary-replace":
            commData["type"] = "researcherDiary"
            if (typeSpecific === "researcher-diary-replace")
                commData["structure"] = diaryStructure
            commData["title"] = title
            break;
        case "structure-book":
            commData["type"] = "structureBook"
            commData["structure"] = bookStructure
            commData["title"] = title
            break;
    }
    commData = { [action]: [commData] };

    var xhr = new XMLHttpRequest();
    xhr.open("POST", "/data_receiver");
    xhr.setRequestHeader("Content-Type", "application/json");
    xhr.send(JSON.stringify(commData));
}

document.getElementById("add-card-button").addEventListener("click", function () {
    addCommCard(true);
});

function addCommCard(isNew, item) {
    // Add template to top of container
    var template = document.querySelector("#communication-template").content.cloneNode(true);
    var cardContainer = document.getElementById("card-container");
    cardContainer.insertBefore(template, cardContainer.firstChild);

    // Get template elements
    var thisCard = document.getElementById("communication-card");

    var id = thisCard.querySelector("#card-id");

    var warningDiv = thisCard.querySelector("#no-type-warning");

    var cardEnablerDiv = thisCard.querySelector("#card-enabler");
    var cardEnablerSwitch = cardEnablerDiv.querySelector("#card-enabler-switch");
    var cardEnablerLabel = cardEnablerDiv.querySelector("#card-enabler-label");

    var deleteCardButton = thisCard.querySelector("#delete-card-button");

    var confirmDeletionModal = document.getElementById("modal-confirm-deletion");
    var deleteButtonModal = document.getElementById("delete-button-modal");

    var commSelect = thisCard.querySelector("#communication-type");

    var researcherStructDiv = thisCard.querySelector("#researcher-diary-struct");
    var diaryStructure = researcherStructDiv.querySelector("#diary-structure");

    var bookStructDiv = thisCard.querySelector("#structure-books");
    var bookStructure = bookStructDiv.querySelector("#book-structure");

    var toastIconDiv = thisCard.querySelector("#toast-icon");
    var iconNamespace = toastIconDiv.querySelector("#namespace");
    var iconItemId = toastIconDiv.querySelector("#item-id");

    var commTitleDiv = thisCard.querySelector("#title");
    var title = commTitleDiv.querySelector("#title-text");

    var contentDiv = thisCard.querySelector("#content");
    var content = contentDiv.querySelector("#content-text");

    // Setting card
    if (isNew) {
        id.value = "communication-" + (lastId + 1);
        lastId++;
    }
    else {
        id.value = item.id;
        content.value = item.content;
        switch (item.type) {
            case "dialogue":
                commSelect.value = item.type;
                break;
            case "toast":
                commSelect.value = item.icon ? "toast-with-icon" : "toast-simple"
                if (item.icon) {
                    iconNamespace.value = item.icon.split(":")[0]
                    iconItemId.value = item.icon.split(":")[1]
                }
                title.value = item.title;
                break;
            case "researcherDiary":
                commSelect.value = item.structure ? "researcher-diary-replace" : "researcher-diary-new"
                if (item.structure)
                    diaryStructure.value = item.structure
                title.value = item.title;
                break;
            case "structureBook":
                commSelect.value = "structure-book"
                bookStructure.value = item.structure
                title.value = item.title;
                break;
        }
        cardEnablerSwitch.checked = item.active
        enableCard(commSelect.value);
        changeColorAndLabel(item.active);
    }

    deleteCardButton.addEventListener("click", function () {
        if (commSelect.value === "none") {
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

    commSelect.addEventListener("change", function () {
        var selectedComm = commSelect.value;
        if (selectedComm !== "none") {
            enableCard(selectedComm);
            cardEnablerSwitch.checked = false;
            changeColorAndLabel(false);
        }
        else {
            warningDiv.hidden = false;
            cardEnablerDiv.hidden = true;
            thisCard.classList.remove("bg-danger-subtle", "bg-success-subtle");
            thisCard.classList.add("bg-dark-subtle");
            hideElements(commTitleDiv, toastIconDiv, researcherStructDiv, bookStructDiv, contentDiv);
        }
    });

    cardContainer.querySelectorAll('input, select, textarea').forEach(function (element) {
        element.addEventListener('change', function () {
            updateServer(thisCard, commSelect.value !== "none" ? "add" : "remove");
        });
    });

    function changeColorAndLabel(isActive) {
        if (isActive)
            thisCard.classList.remove("bg-dark-subtle", "bg-danger-subtle");
        else
            thisCard.classList.remove("bg-dark-subtle", "bg-success-subtle");
        thisCard.classList.add(isActive ? "bg-success-subtle" : "bg-danger-subtle");
        cardEnablerLabel.innerText = isActive ? "Attiva" : "Non attiva";
    }

    function enableCard(selectedComm) {
        warningDiv.hidden = true;
        cardEnablerDiv.hidden = false;
        switch (selectedComm) {
            case 'dialogue':
                showElements(contentDiv);
                hideElements(commTitleDiv, toastIconDiv, researcherStructDiv, bookStructDiv);
                break;
            case 'toast-simple':
                showElements(commTitleDiv, contentDiv);
                hideElements(toastIconDiv, researcherStructDiv, bookStructDiv);
                break;
            case 'toast-with-icon':
                showElements(commTitleDiv, toastIconDiv, contentDiv);
                hideElements(researcherStructDiv, bookStructDiv);
                break;
            case 'researcher-diary-new':
                showElements(commTitleDiv, contentDiv);
                hideElements(toastIconDiv, researcherStructDiv, bookStructDiv);
                break;
            case 'researcher-diary-replace':
                showElements(commTitleDiv, researcherStructDiv, contentDiv);
                hideElements(toastIconDiv, bookStructDiv);
                break;
            case 'structure-book':
                showElements(commTitleDiv, bookStructDiv, contentDiv);
                hideElements(toastIconDiv, researcherStructDiv);
                break;
        }
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