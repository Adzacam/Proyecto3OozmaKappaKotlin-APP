<?php
// ==========================================
// create_user.php
// Ubicación: htdocs/develarq_api/Users/create_user.php
// ==========================================
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

require_once '../db_config/database.php';

// Obtener datos JSON
$data = json_decode(file_get_contents("php://input"));

// Validar datos requeridos
if (empty($data->name) || empty($data->apellido) || empty($data->email) || 
    empty($data->password) || empty($data->rol)) {
    echo json_encode([
        'success' => false,
        'message' => 'Todos los campos son requeridos'
    ]);
    exit;
}

try {
    $database = new Database();
    $db = $database->getConnection();

    // Verificar si el email ya existe
    $checkQuery = "SELECT id FROM users WHERE email = :email AND eliminado = 0";
    $checkStmt = $db->prepare($checkQuery);
    $checkStmt->bindParam(':email', $data->email);
    $checkStmt->execute();

    if ($checkStmt->rowCount() > 0) {
        echo json_encode([
            'success' => false,
            'message' => 'El email ya está registrado'
        ]);
        exit;
    }

    // Insertar nuevo usuario
    $query = "INSERT INTO users (name, apellido, email, password, telefono, rol, estado, created_at) 
              VALUES (:name, :apellido, :email, :password, :telefono, :rol, 'activo', NOW())";
    
    $stmt = $db->prepare($query);
    
    // Hash de la contraseña
    $hashedPassword = password_hash($data->password, PASSWORD_BCRYPT);
    
    $stmt->bindParam(':name', $data->name);
    $stmt->bindParam(':apellido', $data->apellido);
    $stmt->bindParam(':email', $data->email);
    $stmt->bindParam(':password', $hashedPassword);
    $stmt->bindParam(':telefono', $data->telefono);
    $stmt->bindParam(':rol', $data->rol);
    
    if ($stmt->execute()) {
        $userId = $db->lastInsertId();
        
        echo json_encode([
            'success' => true,
            'message' => 'Usuario creado exitosamente',
            'data' => [
                'id' => $userId,
                'name' => $data->name,
                'apellido' => $data->apellido,
                'email' => $data->email,
                'rol' => $data->rol
            ]
        ]);
    } else {
        echo json_encode([
            'success' => false,
            'message' => 'Error al crear el usuario'
        ]);
    }

} catch (PDOException $e) {
    echo json_encode([
        'success' => false,
        'message' => 'Error: ' . $e->getMessage()
    ]);
}
?>