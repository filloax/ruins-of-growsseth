let lastId;

document.addEventListener("DOMContentLoaded", async function () {
    const responseForLastId = await fetch("/last_id");
    lastId = parseInt(await responseForLastId.text());
    const responseForServerData = await fetch("/server_data");
    let serverData = await responseForServerData.json();
    serverData.forEach(item => {
        if (item.type === "structure")
            addStructureCard(false, item);
    });
});

async function updateServer(card, action) {
    let id = card.querySelector("#card-id").value;
    let structure = card.querySelector("#structure-type").value;
    let golemVariant = card.querySelector("#golem-select").value;
    let golemZombie = card.querySelector("#golem-zombie-switch").checked;
    let x = card.querySelector("#x-coord").value;
    let y = card.querySelector("#y-coord").value;
    let z = card.querySelector("#z-coord").value;
    let rotation = card.querySelector("#rotation-select").value;
    let active = card.querySelector("#card-enabler-switch").checked;

    let formattedStructure = structure;
    if (structure === "growsseth:golem_variants") {
        if (golemZombie)
            formattedStructure = formattedStructure + "/zombie_" + golemVariant;
        else
            formattedStructure = formattedStructure + "/" + golemVariant;
    }
    let structureData = {
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

    let xhr = new XMLHttpRequest();
    xhr.open("POST", "/data_receiver");
    xhr.setRequestHeader("Content-Type", "application/json");
    xhr.send(JSON.stringify(structureData));
}

document.getElementById("add-card-button").addEventListener("click", function () {
    addStructureCard(true);
});

function addStructureCard(isNew, item) {
    // Add template to top of container
    let template = document.querySelector("#structure-template").content.cloneNode(true);
    let cardContainer = document.getElementById("card-container");
    cardContainer.insertBefore(template, cardContainer.firstChild);

    // Get template elements
    let thisCard = document.getElementById("structure-card");

    let id = thisCard.querySelector("#card-id");

    let warningDiv = thisCard.querySelector("#no-type-warning");

    let cardEnablerDiv = thisCard.querySelector("#card-enabler");
    let cardEnablerSwitch = cardEnablerDiv.querySelector("#card-enabler-switch");
    let cardEnablerLabel = cardEnablerDiv.querySelector("#card-enabler-label");

    let deleteCardButton = thisCard.querySelector("#delete-card-button");

    let confirmDeletionModal = document.getElementById("modal-confirm-deletion");
    let deleteButtonModal = document.getElementById("delete-button-modal");

    let structurePreview = thisCard.querySelector("#structure-preview");

    let structureSelect = thisCard.querySelector("#structure-type");

    let golemVariantDiv = thisCard.querySelector("#golem-variant");
    let golemVariantSelect = golemVariantDiv.querySelector("#golem-select");
    let golemZombieSwitch = golemVariantDiv.querySelector("#golem-zombie-switch");

    let coordinatesDiv = thisCard.querySelector("#coordinates");
    let x = coordinatesDiv.querySelector("#x-coord");
    let y = coordinatesDiv.querySelector("#y-coord");
    let z = coordinatesDiv.querySelector("#z-coord");

    let rotationDiv = thisCard.querySelector("#rotation");
    let rotation = rotationDiv.querySelector("#rotation-select");

    // Setting card
    if (isNew) {
        id.value = "structure-" + (lastId + 1)
        lastId++;
    }
    else {
        id.value = item.id;
        structureSelect.value = item.structure;
        if (item.structure.includes("growsseth:golem_variants")) {
            structureSelect.value = item.structure.split("/")[0];
            let golemVariant = item.structure.split("/")[1]
            golemVariantSelect.value = golemVariant.replace("zombie_", "");
            golemZombieSwitch.checked = item.structure.includes("/zombie_");
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
        let selectedStructure = structureSelect.value;
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
        updatePreview("growsseth:golem_variants")
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
        cardEnablerLabel.innerText = isActive ? activeText : notActiveText;
    }

    function enableCard(selectedStructure) {
        warningDiv.hidden = true;
        cardEnablerDiv.hidden = false;
        golemVariantDiv.hidden = (selectedStructure !== "growsseth:golem_variants");
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
        if (selectedStructure === "golem_variants")
            selectedPreview = selectedPreview.replace("none", "golem_variants/" + golemVariantSelect.value);
        else
            selectedPreview = selectedPreview.replace("none", selectedStructure);
        structurePreview.src = selectedPreview;
        zombieImgFilter();
    }

    function zombieImgFilter() {
        if (structureSelect.value === "growsseth:golem_variants" && golemZombieSwitch.checked)
            structurePreview.style.filter = "sepia(100%)";
        else
            structurePreview.style.filter = "none";
    }
}