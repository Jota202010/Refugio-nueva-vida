document.addEventListener("DOMContentLoaded", () => {
  const propertyInputs = document.querySelectorAll('input[name="propiedad"]');
  const permitsGroup = document.getElementById("permisoMascotasGroup");
  const permitsInputs = permitsGroup.querySelectorAll('input[name="permitenMascotas"]');
  const peopleInput = document.getElementById("numPersonas");
  const agreementGroup = document.getElementById("acuerdoHogarGroup");
  const agreementInputs = agreementGroup.querySelectorAll('input[name="todosAcuerdo"]');
  const previousDogsInputs = document.querySelectorAll('input[name="perrosAntes"]');
  const previousPetsGroup = document.getElementById("mascotasAnterioresGroup");
  const previousPetsText = previousPetsGroup.querySelector("textarea");

  function setConditionalGroup(group, inputs, visible, required = false) {
    group.hidden = !visible;
    inputs.forEach((input, index) => {
      input.disabled = !visible;
      input.required = visible && required && index === 0;
      if (!visible) input.checked = false;
    });
  }

  function updatePermitsQuestion() {
    const rented = document.querySelector('input[name="propiedad"]:checked')?.value === "alquilada";
    setConditionalGroup(permitsGroup, permitsInputs, rented, true);
  }

  function updateHouseholdAgreement() {
    const people = Number.parseInt(peopleInput.value, 10);
    setConditionalGroup(agreementGroup, agreementInputs, Number.isInteger(people) && people > 1, true);
  }

  function updatePreviousPetsQuestion() {
    const hadDogs = document.querySelector('input[name="perrosAntes"]:checked')?.value === "true";
    previousPetsGroup.hidden = !hadDogs;
    previousPetsText.disabled = !hadDogs;
    if (!hadDogs) previousPetsText.value = "";
  }

  propertyInputs.forEach((input) => input.addEventListener("change", updatePermitsQuestion));
  peopleInput.addEventListener("input", updateHouseholdAgreement);
  previousDogsInputs.forEach((input) => input.addEventListener("change", updatePreviousPetsQuestion));

  updatePermitsQuestion();
  updateHouseholdAgreement();
  updatePreviousPetsQuestion();
});
