<?php
// ==========================================
// get_projects.php
// UBICACIÓN: htdocs/develarq_api/Proyectos/get_projects.php
// ==========================================
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

require_once '../db_config/database.php';

try {
    $database = new Database();
    $db = $database->getConnection();

    // Obtener el usuario actual desde el token (simulado por ahora)
    // TODO: Implementar autenticación real con JWT
    $currentUserId = isset($_GET['user_id']) ? $_GET['user_id'] : null;
    $currentUserRole = isset($_GET['user_rol']) ? $_GET['user_rol'] : 'cliente';

    // Query base
    $query = "SELECT 
                p.id,
                p.nombre,
                p.descripcion,
                p.estado,
                p.fecha_inicio,
                p.fecha_fin,
                p.cliente_id,
                p.responsable_id,
                c.name as cliente_nombre,
                r.name as responsable_nombre
              FROM proyectos p
              LEFT JOIN users c ON p.cliente_id = c.id
              LEFT JOIN users r ON p.responsable_id = r.id
              WHERE p.eliminado = 0";

    // Filtrar según rol del usuario
    if ($currentUserRole === 'cliente' && $currentUserId) {
        $query .= " AND p.cliente_id = :user_id";
    } elseif (in_array($currentUserRole, ['arquitecto', 'ingeniero']) && $currentUserId) {
        // Proyectos donde tiene permiso de edición
        $query .= " AND (p.id IN (
                        SELECT proyecto_id 
                        FROM proyectos_usuarios 
                        WHERE user_id = :user_id AND permiso = 'editar'
                    ) OR p.responsable_id = :user_id)";
    }
    // Admin ve todos los proyectos (no hay filtro adicional)

    $query .= " ORDER BY p.created_at DESC";

    $stmt = $db->prepare($query);

    if ($currentUserId && $currentUserRole !== 'admin') {
        $stmt->bindParam(':user_id', $currentUserId);
    }

    $stmt->execute();

    $proyectos = [];
    while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
        $proyectos[] = [
            'id' => (int) $row['id'],
            'nombre' => $row['nombre'],
            'descripcion' => $row['descripcion'],
            'estado' => $row['estado'],
            'fecha_inicio' => $row['fecha_inicio'],
            'fecha_fin' => $row['fecha_fin'],
            'cliente_id' => (int) $row['cliente_id'],
            'responsable_id' => (int) $row['responsable_id'],
            'cliente' => [
                'id' => (int) $row['cliente_id'],
                'name' => $row['cliente_nombre']
            ],
            'responsable' => [
                'id' => (int) $row['responsable_id'],
                'name' => $row['responsable_nombre']
            ],
            'es_responsable' => ($currentUserId == $row['responsable_id'])
        ];
    }

    echo json_encode([
        'success' => true,
        'message' => 'Proyectos obtenidos exitosamente',
        'data' => $proyectos
    ]);

} catch (PDOException $e) {
    echo json_encode([
        'success' => false,
        'message' => 'Error: ' . $e->getMessage(),
        'data' => null
    ]);
}
?>