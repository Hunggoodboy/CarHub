/**
 * Register Form Validation Script
 * Handles error message display and password confirmation validation
 */

document.addEventListener('DOMContentLoaded', function () {
    // Display error message if registration failed
    displayErrorMessage();

    // Setup password validation
    setupPasswordValidation();
});

/**
 * Display error message from URL parameters if registration failed
 */
function displayErrorMessage() {
    const params = new URLSearchParams(window.location.search);
    if (params.has('error')) {
        const errorElement = document.getElementById('register-error');
        if (errorElement) {
            errorElement.style.display = 'block';

            const errorMsg = params.get('errorMessage');
            if (errorMsg) {
                const errorMessageElement = document.getElementById('error-message');
                if (errorMessageElement) {
                    errorMessageElement.textContent = decodeURIComponent(errorMsg);
                }
            }
        }
    }
}

/**
 * Setup real-time password confirmation validation
 */
function setupPasswordValidation() {
    const passwordInput = document.querySelector('input[name="password"]');
    const confirmPasswordInput = document.querySelector('input[name="confirmPassword"]');

    if (!passwordInput || !confirmPasswordInput) {
        console.warn('Password inputs not found');
        return;
    }

    // Validate on change
    confirmPasswordInput.addEventListener('change', validatePasswordMatch);

    // Also validate on input for real-time feedback
    confirmPasswordInput.addEventListener('input', validatePasswordMatch);
}

/**
 * Validate that password and confirm password match
 */
function validatePasswordMatch() {
    const passwordInput = document.querySelector('input[name="password"]');
    const confirmPasswordInput = document.querySelector('input[name="confirmPassword"]');

    if (passwordInput.value !== confirmPasswordInput.value) {
        confirmPasswordInput.setCustomValidity('Mật khẩu nhập lại không khớp');
        confirmPasswordInput.classList.add('is-invalid');
    } else {
        confirmPasswordInput.setCustomValidity('');
        confirmPasswordInput.classList.remove('is-invalid');
    }
}
