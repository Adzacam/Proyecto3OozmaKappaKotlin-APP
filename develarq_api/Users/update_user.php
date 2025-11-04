
<?php
// ==========================================
// update_user.php MEJORADO con validación de contraseña
// Ubicación: htdocs/develarq_api/Users/update_user.php
// ==========================================
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: PUT, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

require_once '../db_config/database.php';

$data = json_decode(file_get_contents("php://input"));

// Validar datos requeridos
if (empty($data->id) || empty($data->name) || empty($data->apellido) || 
    empty($data->email) || empty($data->rol)) {
    echo json_encode([
        'success' => false,
        'message' => 'Datos incompletos'
    ]);
    exit;
}

// Validar formato de email
if (!filter_var($data->email, FILTER_VALIDATE_EMAIL)) {
    echo json_encode([
        'success' => false,
        'message' => 'Formato de email inválido'
    ]);
    exit;
}

// Si se proporciona una nueva contraseña, validarla
if (isset($data->password) && !empty($data->password)) {
    // Validar contraseña: 8-20 caracteres, mayúscula, minúscula, número, símbolo
    $password = $data->password;
    
    if (strlen($password) < 8 || strlen($password) > 20) {
        echo json_encode([
            'success' => false,
            'message' => 'La contraseña debe tener entre 8 y 20 caracteres'
        ]);
        exit;
    }
    
    if (!preg_match('/[A-Z]/', $password)) {
        echo json_encode([
            'success' => false,
            'message' => 'La contraseña debe contener al menos una mayúscula'
        ]);
        exit;
    }
    
    if (!preg_match('/[a-z]/', $password)) {
        echo json_encode([
            'success' => false,
            'message' => 'La contraseña debe contener al menos una minúscula'
        ]);
        exit;
    }
    
    if (!preg_match('/\d/', $password)) {
        echo json_encode([
            'success' => false,
            'message' => 'La contraseña debe contener al menos un número'
        ]);
        exit;
    }
    
    if (!preg_match('/[@$!%*?&]/', $password)) {
        echo json_encode([
            'success' => false,
            'message' => 'La contraseña debe contener al menos un símbolo (@$!%*?&)'
        ]);
        exit;
    }
    
    // Validar espacios
    if (preg_match('/^\s|\s$|\s\s/', $password)) {
        echo json_encode([
            'success' => false,
            'message' => 'La contraseña no puede tener espacios al inicio, final o dobles espacios'
        ]);
        exit;
    }
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

    // Preparar query de actualización
    if (isset($data->password) && !empty($data->password)) {
        // Actualizar con contraseña
        $query = "UPDATE users 
                  SET name = :name, 
                      apellido = :apellido, 
                      email = :email, 
                      telefono = :telefono, 
                      rol = :rol,
                      password = :password,
                      updated_at = NOW()
                  WHERE id = :id AND eliminado = 0";
        
        $stmt = $db->prepare($query);
        $hashedPassword = password_hash($data->password, PASSWORD_BCRYPT);
        $stmt->bindParam(':password', $hashedPassword);
    } else {
        // Actualizar sin cambiar contraseña
        $query = "UPDATE users 
                  SET name = :name, 
                      apellido = :apellido, 
                      email = :email, 
                      telefono = :telefono, 
                      rol = :rol,
                      updated_at = NOW()
                  WHERE id = :id AND eliminado = 0";
        
        $stmt = $db->prepare($query);
    }
    
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
                'telefono' => $data->telefono,
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