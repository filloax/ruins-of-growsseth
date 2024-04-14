var lastId;

document.addEventListener("DOMContentLoaded", async function () {
    const responseForLastId = await fetch("/last_id");
    lastId = parseInt(await responseForLastId.text());
    const responseForServerData = await fetch("/server_data");
    var serverData = await responseForServerData.json();
    serverData.forEach(item => {
        if (item.type === "structure")
            addStructureCard(false, item);
    });
});

async function updateServer(card, action) {
    var id = card.querySelector("#card-id").value;
    var structure = card.querySelector("#structure-type").value;
    var golemVariant = card.querySelector("#golem-select").value;
    var golemZombie = card.querySelector("#golem-zombie-switch").checked;
    var x = card.querySelector("#x-coord").value;
    var y = card.querySelector("#y-coord").value;
    var z = card.querySelector("#z-coord").value;
    var rotation = card.querySelector("#rotation-select").value;
    var active = card.querySelector("#card-enabler-switch").checked;

    var formattedStructure = structure;
    if (structure === "growsseth:golem_house") {
        formattedStructure = structure + "/" + golemVariant;
        if (golemZombie)
            formattedStructure = formattedStructure + "-zombie";
    }
    var structureData = {
        "id": id,
        "type": "structure",
        "structure": formattedStructure,
        "x": parseInt(x) || 0,
        "y": parseInt(y) || 0,
        "z": parseInt(z) || 0,
        "active": active
    };
    if (rotation !== "auto")
        structureData["rotation"] = rotation;
    structureData = { [action]: [structureData] };

    var xhr = new XMLHttpRequest();
    xhr.open("POST", "/data_receiver");
    xhr.setRequestHeader("Content-Type", "application/json");
    xhr.send(JSON.stringify(structureData));
}

document.getElementById("add-card-button").addEventListener("click", function () {
    addStructureCard(true);
});

function addStructureCard(isNew, item) {
    // Add template to top of container
    var template = document.querySelector("#structure-template").content.cloneNode(true);
    var cardContainer = document.getElementById("card-container");
    cardContainer.insertBefore(template, cardContainer.firstChild);

    // Get template elements
    var thisCard = document.getElementById("structure-card");

    var id = thisCard.querySelector("#card-id");

    var warningDiv = thisCard.querySelector("#no-type-warning");

    var cardEnablerDiv = thisCard.querySelector("#card-enabler");
    var cardEnablerSwitch = cardEnablerDiv.querySelector("#card-enabler-switch");
    var cardEnablerLabel = cardEnablerDiv.querySelector("#card-enabler-label");

    var deleteCardButton = thisCard.querySelector("#delete-card-button");

    var confirmDeletionModal = document.getElementById("modal-confirm-deletion");
    var deleteButtonModal = document.getElementById("delete-button-modal");

    var structurePreview = thisCard.querySelector("#structure-preview");

    var structureSelect = thisCard.querySelector("#structure-type");

    var golemVariantDiv = thisCard.querySelector("#golem-variant");
    var golemVariantSelect = golemVariantDiv.querySelector("#golem-select");
    var golemZombieSwitch = golemVariantDiv.querySelector("#golem-zombie-switch");

    var coordinatesDiv = thisCard.querySelector("#coordinates");
    var x = coordinatesDiv.querySelector("#x-coord");
    var y = coordinatesDiv.querySelector("#y-coord");
    var z = coordinatesDiv.querySelector("#z-coord");

    var rotationDiv = thisCard.querySelector("#rotation");
    var rotation = rotationDiv.querySelector("#rotation-select");

    // Setting card
    if (isNew) {
        id.value = "structure-" + (lastId + 1)
        lastId++;
    }
    else {
        id.value = item.id;
        structureSelect.value = item.structure;
        if (item.structure.includes("growsseth:golem_house")) {
            structureSelect.value = item.structure.split("/")[0];
            var golemVariant = item.structure.split("/")[1]
            golemVariantSelect.value = golemVariant.replace("-zombie", "");
            golemZombieSwitch.checked = item.structure.endsWith("-zombie");
        }
        x.value = item.x;
        y.value = item.y;
        z.value = item.z;
        rotation.value = item.rotation ? item.rotation : "auto";
        cardEnablerSwitch.checked = item.active

        enableCard(structureSelect.value)
        changeColorAndLabel(item.active);
        updatePreview(structureSelect.value)
    }

    structureSelect.addEventListener("change", function () {
        var selectedStructure = structureSelect.value;
        if (selectedStructure !== "growsseth:none") {
            enableCard(selectedStructure)
            cardEnablerSwitch.checked = false;
            changeColorAndLabel(false)
            updatePreview(selectedStructure)
        }
        else {
            disableCard()
            thisCard.classList.remove("bg-danger-subtle", "bg-success-subtle");
            thisCard.classList.add("bg-dark-subtle");
            structurePreview.src = defaultPreview;
        }
    });

    golemVariantSelect.addEventListener("change", function () {
        updatePreview("growsseth:golem_house")
    });

    golemZombieSwitch.addEventListener("change", function () {
        zombieImgFilter();
    });

    deleteCardButton.addEventListener("click", function () {
        if (structureSelect.value === "growsseth:none") {
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

    cardContainer.querySelectorAll('input, select').forEach(function (element) {
        element.addEventListener('change', function () {
            updateServer(thisCard, structureSelect.value !== "growsseth:none" ? "add" : "remove");
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

    function enableCard(selectedStructure) {
        warningDiv.hidden = true;
        cardEnablerDiv.hidden = false;
        golemVariantDiv.hidden = (selectedStructure !== "growsseth:golem_house");
        coordinatesDiv.hidden = false;
        rotationDiv.hidden = false;
    }

    function disableCard() {
        warningDiv.hidden = false;
        cardEnablerDiv.hidden = true;
        golemVariantDiv.hidden = true;
        coordinatesDiv.hidden = true;
        rotationDiv.hidden = true;
    }

    function updatePreview(selectedStructure) {
        selectedStructure = selectedStructure.split(":")[1]
        selectedPreview = defaultPreview;
        if (selectedStructure === "golem_house")
            selectedPreview = selectedPreview.replace("none", "golem_house/" + golemVariantSelect.value);
        else
            selectedPreview = selectedPreview.replace("none", selectedStructure);
        structurePreview.src = selectedPreview;
        zombieImgFilter();
    }

    function zombieImgFilter() {
        if (structureSelect.value === "growsseth:golem_house" && golemZombieSwitch.checked)
            structurePreview.style.filter = "sepia(100%)";
        else
            structurePreview.style.filter = "none";
    }
}