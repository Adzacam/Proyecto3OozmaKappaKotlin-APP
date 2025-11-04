<?php
// Directorio base de la API para construir rutas relativas
header("Content-Type: text/html; charset=UTF-8");
include_once '../db_config/database.php';

// --- CONEXIÓN Y VERIFICACIÓN DE TOKEN ---
$database = new Database();
$db = $database->getConnection();

$message = ""; 
$token = $_GET['token'] ?? null;
$status = $_GET['status'] ?? null;
$email = null; // Inicializar email

// Si NO hay token Y NO estamos en el estado de éxito (redirección PRG), muere.
if (!$token && $status !== 'success') {
    die("<div style='font-family: Arial, sans-serif; text-align: center; margin-top: 50px;'><h3 style='color:red;'>Token no proporcionado.</h3></div>");
}

// Lógica de token solo se ejecuta si un token está presente
if ($token) {
    
    // Si estamos en la redirección de éxito, no necesitamos verificar el token en la DB.
    if ($status !== 'success') {
        // Ejecución normal (petición inicial o error POST): Verificar el token en DB.

        // Verificar si el token existe y si está expirado (e.g., más de 10 minutos)
        $query = "SELECT email, created_at FROM password_reset_tokens WHERE token = :token AND created_at > DATE_SUB(NOW(), INTERVAL 10 MINUTE)";
        $stmt = $db->prepare($query);
        $stmt->bindParam(':token', $token);
        $stmt->execute();

        if ($stmt->rowCount() === 0) {
            // Si el token no existe o expiró, muere.
            die("<div style='font-family: Arial, sans-serif; text-align: center; margin-top: 50px;'><h3 style='color:red;'>El enlace no es válido o ha expirado.</h3></div>");
        }
        $tokenData = $stmt->fetch(PDO::FETCH_ASSOC);
        $email = $tokenData['email'];
    } else {
    
        $query = "SELECT email FROM password_reset_tokens WHERE token = :token";
        $stmt = $db->prepare($query);
        $stmt->bindParam(':token', $token);
        $stmt->execute();
        $tokenData = $stmt->fetch(PDO::FETCH_ASSOC);
        // Si no está el token, asumimos que se eliminó con éxito
        if ($tokenData) {
            $email = $tokenData['email'];
        }
        // Nota: Si el email es null aquí, no es crítico ya que el formulario no se muestra.
    }

    // --- PROCESAMIENTO DEL FORMULARIO POST ---
    if ($_SERVER['REQUEST_METHOD'] === 'POST') {
        $newPassword = $_POST['password'] ?? '';
        $confirmPassword = $_POST['confirm_password'] ?? '';

        // Si el email no se pudo establecer, es un error interno, no deberíamos continuar.
        if (!$email) {
             die("<div style='font-family: Arial, sans-serif; text-align: center; margin-top: 50px;'><h3 style='color:red;'>Error de sesión. Intenta de nuevo.</h3></div>");
        }

        // El mismo RegEx del código original
        $PASSWORD_REGEX = '/^(?=.*[A-Z])(?=.*[a-z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,20}$/';

        // Obtener contraseña actual (para evitar la misma contraseña)
        $userQuery = "SELECT password FROM users WHERE email = :email";
        $userStmt = $db->prepare($userQuery);
        $userStmt->bindParam(':email', $email);
        $userStmt->execute();
        $userData = $userStmt->fetch(PDO::FETCH_ASSOC);
        $currentHash = $userData['password'] ?? '';

        // Validaciones
        if (strlen($newPassword) < 8 || strlen($newPassword) > 20) {
            $message = "<p class='error'>❌ La contraseña debe tener entre 8 y 20 caracteres.</p>";
        } elseif (!preg_match($PASSWORD_REGEX, $newPassword)) {
            $message = "<p class='error'>❌ Debe incluir mayúsculas, minúsculas, un número y un símbolo (@$!%*?&).</p>";
        } elseif (preg_match('/^\s|\s$|\s\s/', $newPassword)) {
            $message = "<p class='error'>❌ No se permiten espacios al inicio, final ni dobles espacios.</p>";
        } elseif ($newPassword !== $confirmPassword) {
            $message = "<p class='error'>❌ Las contraseñas no coinciden.</p>";
        } elseif (password_verify($newPassword, $currentHash)) {
            $message = "<p class='error'>⚠️ No puedes usar la misma contraseña anterior.</p>";
        } else {
            // --- PROCESO DE ACTUALIZACIÓN EXITOSO ---
            $hashedPassword = password_hash($newPassword, PASSWORD_DEFAULT);
            $updateQuery = "UPDATE users SET password = :password WHERE email = :email";
            $updateStmt = $db->prepare($updateQuery);
            $updateStmt->bindParam(':password', $hashedPassword);
            $updateStmt->bindParam(':email', $email);

            if ($updateStmt->execute()) {
                // Eliminar token para evitar reuso
                $deleteQuery = "DELETE FROM password_reset_tokens WHERE email = :email";
                $deleteStmt = $db->prepare($deleteQuery);
                $deleteStmt->bindParam(':email', $email);
                $deleteStmt->execute();

                // Redirección con el token Y el status de éxito.
                header("Location: index.php?token=" . $token . "&status=success");
                exit(); 
            } else {
                $message = "<p class='error'>❌ Error al actualizar la contraseña.</p>";
            }
        }
    }
}

// --- Manejo del mensaje de éxito después de la redirección PRG ---
if ($status === 'success') {
    $message = "<p class='success'>✅ Contraseña restablecida exitosamente. Ya puedes iniciar sesión.</p>";
}
?>

<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>Restablecer Contraseña</title>
    <link rel="stylesheet" href="style.css"> 
</head>
<body>
    <div class="container">
        <h2>Restablecer tu contraseña</h2>
        <?= $message ?>

        <?php if ($status !== 'success' && $token): ?>
        <form method="POST">
            <label for="password">Nueva contraseña:</label>
            <input type="password" name="password" id="password" required>

            <div class="requirements">
                <p id="len" class="requirement">• Entre 8 y 20 caracteres</p>
                <p id="upper" class="requirement">• Al menos una letra mayúscula</p>
                <p id="lower" class="requirement">• Al menos una letra minúscula</p>
                <p id="num" class="requirement">• Al menos un número</p>
                <p id="sym" class="requirement">• Al menos un símbolo (@$!%*?&)</p>
                <p id="spc" class="requirement">• Sin espacios al inicio, final ni dobles</p>
            </div>

            <label for="confirm_password">Confirmar contraseña:</label>
            <input type="password" name="confirm_password" id="confirm_password" required>

            <button id="submitBtn" type="submit">Actualizar Contraseña</button>
        </form>
        <?php endif; ?>
    </div>
    <script src="validate.js"></script>
</body>
</html>