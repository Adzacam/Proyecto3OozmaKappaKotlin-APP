<?php
// ==========================================
// restore_user.php
// Ubicación: htdocs/develarq_api/Users/restore_user.php
// ==========================================
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

require_once '../db_config/database.php';

$data = json_decode(file_get_contents("php://input"));

if (empty($data->id)) {
    echo json_encode([
        'success' => false,
        'message' => 'ID de usuario requerido'
    ]);
    exit;
}

try {
    $database = new Database();
    $db = $database->getConnection();

    // Restaurar usuario: marcar como no eliminado
    $query = "UPDATE users 
              SET eliminado = 0, 
                  estado = 'activo',
                  updated_at = NOW()
              WHERE id = :id AND eliminado = 1";
    
    $stmt = $db->prepare($query);
    $stmt->bindParam(':id', $data->id);
    
    if ($stmt->execute()) {
        echo json_encode([
            'success' => true,
            'message' => 'Usuario restaurado exitosamente'
        ]);
    } else {
        echo json_encode([
            'success' => false,
            'message' => 'Error al restaurar el usuario'
        ]);
    }

} catch (PDOException $e) {
    echo json_encode([
        'success' => false,
        'message' => 'Error: ' . $e->getMessage()
    ]);
}
?>
