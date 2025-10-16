document.addEventListener("DOMContentLoaded", () => {
    // Referencias a elementos
    const passwordInput = document.getElementById('password');

    // Mapeo de reglas y sus expresiones regulares
    const validations = [
        { regex: /^.{8,20}$/, id: 'len' },             // 8 a 20 caracteres
        { regex: /[A-Z]/, id: 'upper' },               // Mayúscula
        { regex: /[a-z]/, id: 'lower' },               // Minúscula
        { regex: /\d/, id: 'num' },                    // Número
        { regex: /[@$!%*?&]/, id: 'sym' },             // Símbolo
        // Sin espacios al inicio/final Y sin dobles espacios en el medio
        { regex: /^\S(?!.*\s\s).*?\S$|^\S$|^$/, id: 'spc' }
    ];

    // Función para actualizar los mensajes de validación
    const updateValidationMessages = (value) => {
        validations.forEach(v => {
            const el = document.getElementById(v.id);
            if (el) {
                // Si pasa el regex, color de éxito; si falla, color de error.
                el.style.color = v.regex.test(value) ? '#4ADE80' : '#F87171';
            }
        });
    };

    // Escucha el evento 'input' en el campo de contraseña
    if (passwordInput) {
        passwordInput.addEventListener('input', (event) => {
            const value = event.target.value;
            updateValidationMessages(value);
        });
        
        // Ejecutar la validación al cargar si ya tiene un valor (ej: después de un error POST)
        updateValidationMessages(passwordInput.value);
    }
});