<?php
// ==========================================
// get_users.php
// Ubicación: htdocs/develarq_api/Users/get_users.php
// ==========================================
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

require_once '../db_config/database.php';

try {
    $database = new Database();
    $db = $database->getConnection();

    // Obtener solo usuarios no eliminados y activos
    $query = "SELECT id, name, apellido, email, telefono, rol, estado, created_at 
              FROM users 
              WHERE eliminado = 0 
              ORDER BY created_at DESC";
    
    $stmt = $db->prepare($query);
    $stmt->execute();
    
    $users = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    echo json_encode([
        'success' => true,
        'data' => $users,
        'message' => 'Usuarios obtenidos correctamente'
    ]);

} catch (PDOException $e) {
    echo json_encode([
        'success' => false,
        'message' => 'Error al obtener usuarios: ' . $e->getMessage()
    ]);
}
?>