<?php
header("Content-Type: text/html; charset=UTF-8");
include_once '../db_config/database.php';

$database = new Database();
$db = $database->getConnection();

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    die("<p style='color:red;'>Método no permitido.</p>");
}

$email = $_POST['email'] ?? '';
$newPassword = $_POST['password'] ?? '';
$confirmPassword = $_POST['confirm_password'] ?? '';

$PASSWORD_REGEX = '/^(?=.*[A-Z])(?=.*[a-z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,20}$/';

if (strlen($newPassword) < 8 || strlen($newPassword) > 20) {
    die("<p style='color:red;'>La contraseña debe tener entre 8 y 20 caracteres.</p>");
}
if (!preg_match($PASSWORD_REGEX, $newPassword)) {
    die("<p style='color:red;'>Debe incluir mayúsculas, minúsculas, un número y un símbolo (@$!%*?&).</p>");
}
if (preg_match('/^\s|\s$|  /', $newPassword)) {
    die("<p style='color:red;'>No se permiten espacios al inicio, final ni dobles espacios.</p>");
}
if ($newPassword !== $confirmPassword) {
    die("<p style='color:red;'>Las contraseñas no coinciden.</p>");
}

$hashedPassword = password_hash($newPassword, PASSWORD_DEFAULT);

try {
    $updateQuery = "UPDATE users SET password = :password WHERE email = :email";
    $updateStmt = $db->prepare($updateQuery);
    $updateStmt->bindParam(':password', $hashedPassword);
    $updateStmt->bindParam(':email', $email);

    if ($updateStmt->execute()) {
        $deleteQuery = "DELETE FROM password_reset_tokens WHERE email = :email";
        $deleteStmt = $db->prepare($deleteQuery);
        $deleteStmt->bindParam(':email', $email);
        $deleteStmt->execute();

        echo "<p style='color:green; text-align:center;'>✅ Contraseña restablecida exitosamente. Ya puedes iniciar sesión.</p>";
    } else {
        echo "<p style='color:red; text-align:center;'>Error al actualizar la contraseña.</p>";
    }
} catch (PDOException $e) {
    echo "<p style='color:red;'>Error en la base de datos: " . $e->getMessage() . "</p>";
}
?>
