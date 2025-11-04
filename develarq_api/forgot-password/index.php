<?php
require '../vendor/autoload.php'; // Carga PHPMailer
use PHPMailer\PHPMailer\PHPMailer;
use PHPMailer\PHPMailer\Exception;

include_once '../db_config/database.php';

$database = new Database();
$db = $database->getConnection();

$data = json_decode(file_get_contents("php://input"));

if (empty($data->email)) {
    http_response_code(422);
    echo json_encode(["success" => false, "message" => "El correo es requerido."]);
    exit();
}

$email = $data->email;

// 1. Verificar si el usuario existe
$query = "SELECT id FROM users WHERE email = :email AND eliminado = 0";
$stmt = $db->prepare($query);
$stmt->bindParam(':email', $email);
$stmt->execute();

if ($stmt->rowCount() == 0) {
    // Respuesta genérica
    http_response_code(200);
    echo json_encode(["success" => true, "message" => "Si tu correo está registrado, recibirás un enlace."]);
    exit();
}

// 2. Generar token único
$token = bin2hex(random_bytes(32));

// 3. Guardar token
$deleteQuery = "DELETE FROM password_reset_tokens WHERE email = :email";
$deleteStmt = $db->prepare($deleteQuery);
$deleteStmt->bindParam(':email', $email);
$deleteStmt->execute();

$insertQuery = "INSERT INTO password_reset_tokens (email, token, created_at) VALUES (:email, :token, NOW())";
$insertStmt = $db->prepare($insertQuery);
$insertStmt->bindParam(':email', $email);
$insertStmt->bindParam(':token', $token);

if ($insertStmt->execute()) {

    $mail = new PHPMailer(true);

    try {
        // Configuración del servidor SMTP
        $mail->isSMTP();
        $mail->Host       = 'smtp.gmail.com';
        $mail->SMTPAuth   = true;
        $mail->Username   = 'fidelrey000@gmail.com';
        $mail->Password   = 'qnrwvlmrcovalrso';
        $mail->SMTPSecure = PHPMailer::ENCRYPTION_STARTTLS;
        $mail->Port       = 587;

        // Remitente y destinatario
        $mail->setFrom('no-reply@develarq.com', 'Soporte DevelArq');
        $mail->addAddress($email);

        // Contenido del correo
        $mail->isHTML(true);
        $mail->Subject = 'Restablecer tu contraseña de DevelArq';
        $resetLink = "http://192.168.1.7/develarq_api/reset-password/index.php?token=" . $token;
        $mail->Body = "
            <h2>Hola,</h2>
            <p>Has solicitado restablecer tu contraseña. Haz clic en el enlace siguiente:</p>
            <p><a href='$resetLink'>Restablecer contraseña</a></p>
            <p>Si no solicitaste esto, ignora este correo.</p>
            <br><small>Atentamente,<br>El equipo de DevelArq</small>
        ";

        // Enviar
        $mail->send();
        http_response_code(200);
        echo json_encode(["success" => true, "message" => "Se ha enviado un enlace a tu correo."]);

    } catch (Exception $e) {
        http_response_code(500);
        echo json_encode([
            "success" => false,
            "message" => "Error al enviar el correo: {$mail->ErrorInfo}"
        ]);
    }

} else {
    http_response_code(500);
    echo json_encode(["success" => false, "message" => "Error al procesar la solicitud."]);
}

?>