<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

require_once '../db_config/database.php';

// Verificar autenticación
$headers = getallheaders();
if (!isset($headers['Authorization'])) {
    echo json_encode(['success' => false, 'message' => 'Token no proporcionado']);
    exit;
}

try {
    $pdo = $database->getConnection();
    
    // Obtener todos los proyectos activos
    $query = "
        SELECT 
            id,
            nombre,
            descripcion,
            estado,
            fecha_inicio,
            fecha_fin,
            cliente_id,
            responsable_id
        FROM proyectos
        WHERE eliminado = 0
        ORDER BY nombre ASC
    ";
    
    $stmt = $pdo->query($query);
    $projects = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    echo json_encode([
        'success' => true,
        'data' => $projects
    ]);
    
} catch (PDOException $e) {
    echo json_encode([
        'success' => false,
        'message' => 'Error en el servidor: ' . $e->getMessage()
    ]);
}
?>