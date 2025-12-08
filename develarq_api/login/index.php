<?php
// ==========================================
// login.php (Con Auditoría de Dispositivo)
// ==========================================
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization");

if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    http_response_code(200);
    exit();
}

include_once '../db_config/database.php';
// ✅ IMPORTANTE: Incluir el helper de auditoría
include_once '../db_config/audit_helper.php'; 

$database = new Database();
$db = $database->getConnection();

$data = json_decode(file_get_contents("php://input"));

if (empty($data->email) || empty($data->password)) {
    http_response_code(422);
    echo json_encode(["success" => false, "message" => "Datos inválidos."]);
    exit();
}

try {
    // Buscar al usuario
    $query = "SELECT id, name, apellido, email, password, telefono, rol, estado 
              FROM users 
              WHERE email = :email AND eliminado = 0";

    $stmt = $db->prepare($query);
    $stmt->bindParam(':email', $data->email);
    $stmt->execute();

    $user = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$user || !password_verify($data->password, $user['password'])) {
        http_response_code(401);
        echo json_encode(["success" => false, "message" => "Credenciales incorrectas."]);
        exit();
    }

    if ($user['estado'] !== 'activo') {
        http_response_code(403);
        echo json_encode(["success" => false, "message" => "Usuario inactivo."]);
        exit();
    }

    // Generar Token
    $token = bin2hex(random_bytes(32));
    
    $updateTokenQuery = "UPDATE users SET remember_token = :token, updated_at = NOW() WHERE id = :id";
    $updateStmt = $db->prepare($updateTokenQuery);
    $updateStmt->bindParam(':token', $token);
    $updateStmt->bindParam(':id', $user['id']);
    
    if (!$updateStmt->execute()) {
        throw new Exception("Error al guardar token de sesión");
    }

    // ==========================================
    // ✅ REGISTRAR AUDITORÍA DE INICIO DE SESIÓN
    // ==========================================
    $device_info = [
        'modelo' => isset($data->device_model) ? $data->device_model : 'Web/Desconocido',
        'android' => isset($data->android_version) ? $data->android_version : 'N/A',
        'sdk' => isset($data->sdk_version) ? $data->sdk_version : 'N/A',
        'ip' => obtenerIPCliente() // Función de audit_helper.php
    ];

    $descripcion = "Inicio de sesión exitoso.\n";
    $descripcion .= "Dispositivo: {$device_info['modelo']}\n";
    $descripcion .= "Android: {$device_info['android']} (SDK {$device_info['sdk']})\n";
    $descripcion .= "IP: {$device_info['ip']}";

    // Insertar en auditoría
    // Nota: Usamos 'users' como tabla afectada y el ID del usuario logueado
    $queryAudit = "INSERT INTO auditoria_logs 
                  (user_id, accion, descripcion_detallada, tabla_afectada, id_registro_afectado, ip_address, fecha_accion, created_at) 
                  VALUES 
                  (:uid, 'Inicio de Sesión', :desc, 'users', :ref_id, :ip, NOW(), NOW())";
    
    $stmtAudit = $db->prepare($queryAudit);
    $stmtAudit->execute([
        ':uid' => $user['id'],
        ':desc' => $descripcion,
        ':ref_id' => $user['id'],
        ':ip' => $device_info['ip']
    ]);
    // ==========================================

    $userData = [
        'id' => (int)$user['id'],
        'name' => $user['name'],
        'apellido' => $user['apellido'],
        'email' => $user['email'],
        'telefono' => $user['telefono'],
        'rol' => $user['rol'],
        'estado' => $user['estado']
    ];

    http_response_code(200);
    echo json_encode([
        "success" => true,
        "message" => "Login exitoso",
        "data" => $userData,
        "token" => $token
    ]);

} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(["success" => false, "message" => "Error del servidor: " . $e->getMessage()]);
}
?>