<?php
include_once '../db_config/database.php';

$database = new Database();
$db = $database->getConnection();

$data = json_decode(file_get_contents("php://input"));

// Validaciones básicas
if (empty($data->name) || empty($data->apellido) || empty($data->email) || empty($data->password) || empty($data->rol)) {
    http_response_code(422);
    echo json_encode(["success" => false, "message" => "Datos incompletos."]);
    exit();
}

// Hashear la contraseña
$hashed_password = password_hash($data->password, PASSWORD_BCRYPT);

try {
    $query = "INSERT INTO users (name, apellido, email, password, telefono, rol, estado) 
              VALUES (:name, :apellido, :email, :password, :telefono, :rol, 'activo')";

    $stmt = $db->prepare($query);

    // Bind de los parámetros
    $stmt->bindParam(':name', $data->name);
    $stmt->bindParam(':apellido', $data->apellido);
    $stmt->bindParam(':email', $data->email);
    $stmt->bindParam(':password', $hashed_password);
    $stmt->bindParam(':telefono', $data->telefono);
    $stmt->bindParam(':rol', $data->rol);

    if ($stmt->execute()) {
        http_response_code(201); // Created
        echo json_encode(["success" => true, "message" => "Usuario registrado exitosamente."]);
    } else {
        http_response_code(500);
        echo json_encode(["success" => false, "message" => "No se pudo registrar el usuario."]);
    }
} catch (PDOException $e) {
    // Manejar error de email duplicado
    if ($e->getCode() == 23000) {
        http_response_code(409); // Conflict
        echo json_encode(["success" => false, "message" => "El correo electrónico ya está registrado."]);
    } else {
        http_response_code(500);
        echo json_encode(["success" => false, "message" => "Error al registrar: " . $e->getMessage()]);
    }
}
?>
