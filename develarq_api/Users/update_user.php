<?php
// ==========================================
// update_user.php
// Ubicación: htdocs/develarq_api/Users/update_user.php
// ==========================================
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: PUT, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

require_once '../db_config/database.php';

$data = json_decode(file_get_contents("php://input"));

if (empty($data->id) || empty($data->name) || empty($data->apellido) || 
    empty($data->email) || empty($data->rol)) {
    echo json_encode([
        'success' => false,
        'message' => 'Datos incompletos'
    ]);
    exit;
}

try {
    $database = new Database();
    $db = $database->getConnection();

    // Verificar si el email ya existe en otro usuario
    $checkQuery = "SELECT id FROM users WHERE email = :email AND id != :id AND eliminado = 0";
    $checkStmt = $db->prepare($checkQuery);
    $checkStmt->bindParam(':email', $data->email);
    $checkStmt->bindParam(':id', $data->id);
    $checkStmt->execute();

    if ($checkStmt->rowCount() > 0) {
        echo json_encode([
            'success' => false,
            'message' => 'El email ya está en uso por otro usuario'
        ]);
        exit;
    }

    // Actualizar usuario
    $query = "UPDATE users 
              SET name = :name, 
                  apellido = :apellido, 
                  email = :email, 
                  telefono = :telefono, 
                  rol = :rol,
                  updated_at = NOW()
              WHERE id = :id AND eliminado = 0";
    
    $stmt = $db->prepare($query);
    
    $stmt->bindParam(':id', $data->id);
    $stmt->bindParam(':name', $data->name);
    $stmt->bindParam(':apellido', $data->apellido);
    $stmt->bindParam(':email', $data->email);
    $stmt->bindParam(':telefono', $data->telefono);
    $stmt->bindParam(':rol', $data->rol);
    
    if ($stmt->execute()) {
        echo json_encode([
            'success' => true,
            'message' => 'Usuario actualizado exitosamente',
            'data' => [
                'id' => $data->id,
                'name' => $data->name,
                'apellido' => $data->apellido,
                'email' => $data->email,
                'rol' => $data->rol
            ]
        ]);
    } else {
        echo json_encode([
            'success' => false,
            'message' => 'Error al actualizar el usuario'
        ]);
    }

} catch (PDOException $e) {
    echo json_encode([
        'success' => false,
        'message' => 'Error: ' . $e->getMessage()
    ]);
}
?>
