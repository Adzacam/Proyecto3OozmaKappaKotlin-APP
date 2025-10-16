<?php
// Incluir el archivo de conexión a la base de datos
include_once '../db_config/database.php';

// Obtener la conexión
$database = new Database();
$db = $database->getConnection();

// Leer los datos JSON del cuerpo de la petición
$data = json_decode(file_get_contents("php://input"));

// Validar que los datos no estén vacíos
if (empty($data->email) || empty($data->password)) {
    http_response_code(422); // Unprocessable Entity
    echo json_encode(["success" => false, "message" => "Datos inválidos. Correo y contraseña son requeridos."]);
    exit();
}

try {
    // Buscar al usuario por email
    $query = "SELECT id, name, apellido, email, password, telefono, rol, estado 
              FROM users 
              WHERE email = :email AND eliminado = 0";

    $stmt = $db->prepare($query);
    $stmt->bindParam(':email', $data->email);
    $stmt->execute();

    $user = $stmt->fetch(PDO::FETCH_ASSOC);

    // Si no se encuentra el usuario o la contraseña es incorrecta
    if (!$user || !password_verify($data->password, $user['password'])) {
        http_response_code(401); // Unauthorized
        echo json_encode(["success" => false, "message" => "Credenciales incorrectas."]);
        exit();
    }

    // Verificar si el usuario está activo
    if ($user['estado'] !== 'activo') {
        http_response_code(403); // Forbidden
        echo json_encode(["success" => false, "message" => "Usuario inactivo."]);
        exit();
    }

    // Generar un token simple (para este enfoque, puede ser algo básico)
    $token = bin2hex(random_bytes(32)); 

    // Preparar los datos del usuario para la respuesta
    $userData = [
        'id' => (int)$user['id'],
        'name' => $user['name'],
        'apellido' => $user['apellido'],
        'email' => $user['email'],
        'telefono' => $user['telefono'],
        'rol' => $user['rol'],
        'estado' => $user['estado']
    ];

    // Login exitoso
    http_response_code(200);
    echo json_encode([
        "success" => true,
        "message" => "Login exitoso",
        "data" => $userData,
        "token" => $token // En una app real, este token se gestionaría de forma más segura
    ]);

} catch (Exception $e) {
    http_response_code(500); // Internal Server Error
    echo json_encode(["success" => false, "message" => "Error del servidor: " . $e->getMessage()]);
}
?>