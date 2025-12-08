<?php
// ============================================
// API: CREATE TASK - Crear nueva tarea
// ============================================
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

require_once '../db_config/database.php';
require_once '../db_config/audit_helper.php';

$database = new Database();
$db = $database->getConnection();

// ============================================
// VERIFICAR TOKEN
// ============================================
$headers = getallheaders();
$authHeader = isset($headers['Authorization']) ? $headers['Authorization'] : '';

if (empty($authHeader) || !preg_match('/Bearer\s(\S+)/', $authHeader, $matches)) {
    http_response_code(401);
    echo json_encode(["success" => false, "message" => "Token no proporcionado"]);
    exit();
}

$token = $matches[1];
$usuario = obtenerUsuarioDesdeToken($db, $token);

if (!$usuario) {
    http_response_code(401);
    echo json_encode(["success" => false, "message" => "Token inválido"]);
    exit();
}

// ============================================
// OBTENER DATOS DEL REQUEST
// ============================================
$data = json_decode(file_get_contents("php://input"));

if (!$data) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "Datos inválidos"]);
    exit();
}

// Validar campos obligatorios
if (empty($data->proyecto_id) || empty($data->titulo) || empty($data->prioridad)) {
    http_response_code(400);
    echo json_encode([
        "success" => false,
        "message" => "Faltan campos obligatorios: proyecto_id, titulo, prioridad"
    ]);
    exit();
}

$proyectoId = $data->proyecto_id;
$titulo = trim($data->titulo);
$descripcion = isset($data->descripcion) ? trim($data->descripcion) : null;
$estado = isset($data->estado) ? $data->estado : 'pendiente';
$prioridad = $data->prioridad;
$fechaLimite = isset($data->fecha_limite) ? $data->fecha_limite : null;
$asignadoId = isset($data->asignado_id) ? $data->asignado_id : null;
$creadorId = $usuario['id'];

// Info del dispositivo
$deviceInfo = extraerInfoDispositivo($data);

try {
    $db->beginTransaction();

    // ============================================
    // VERIFICAR PERMISOS EN EL PROYECTO
    // ============================================
    $queryPermiso = "SELECT p.id, p.nombre
                     FROM proyectos p
                     LEFT JOIN proyectos_usuarios pu ON p.id = pu.proyecto_id AND pu.user_id = :user_id
                     WHERE p.id = :proyecto_id 
                     AND p.eliminado = 0
                     AND (
                         p.responsable_id = :user_id
                         OR p.cliente_id = :user_id
                         OR pu.permiso = 'editar'
                         OR :user_rol = 'admin'
                     )";
    
    $stmtPermiso = $db->prepare($queryPermiso);
    $stmtPermiso->execute([
        ':proyecto_id' => $proyectoId,
        ':user_id' => $creadorId,
        ':user_rol' => $usuario['rol']
    ]);

    if ($stmtPermiso->rowCount() === 0) {
        throw new Exception("No tienes permisos para crear tareas en este proyecto");
    }

    $proyecto = $stmtPermiso->fetch(PDO::FETCH_ASSOC);

    // ============================================
    // INSERTAR TAREA
    // ============================================
    $queryInsert = "INSERT INTO tareas (
                        proyecto_id, titulo, descripcion, estado, 
                        prioridad, fecha_limite, asignado_id, creador_id,
                        created_at, updated_at
                    ) VALUES (
                        :proyecto_id, :titulo, :descripcion, :estado,
                        :prioridad, :fecha_limite, :asignado_id, :creador_id,
                        NOW(), NOW()
                    )";

    $stmtInsert = $db->prepare($queryInsert);
    $stmtInsert->execute([
        ':proyecto_id' => $proyectoId,
        ':titulo' => $titulo,
        ':descripcion' => $descripcion,
        ':estado' => $estado,
        ':prioridad' => $prioridad,
        ':fecha_limite' => $fechaLimite,
        ':asignado_id' => $asignadoId,
        ':creador_id' => $creadorId
    ]);

    $tareaId = $db->lastInsertId();

    // ============================================
    // REGISTRAR EN HISTORIAL
    // ============================================
    $queryHistorial = "INSERT INTO tarea_historials (
                          proyecto_id, tarea_id, usuario_id,
                          estado_anterior, estado_nuevo, cambio, fecha_cambio,
                          created_at, updated_at
                       ) VALUES (
                          :proyecto_id, :tarea_id, :usuario_id,
                          NULL, :estado, :cambio, NOW(),
                          NOW(), NOW()
                       )";

    $stmtHistorial = $db->prepare($queryHistorial);
    $stmtHistorial->execute([
        ':proyecto_id' => $proyectoId,
        ':tarea_id' => $tareaId,
        ':usuario_id' => $creadorId,
        ':estado' => $estado,
        ':cambio' => "Creación de la tarea (estado inicial {$estado})"
    ]);

    // ============================================
    // AUDITORÍA
    // ============================================
    $accion = "Creó la tarea '{$titulo}' en el proyecto '{$proyecto['nombre']}'" . 
              ($asignadoId ? " asignada al usuario #{$asignadoId}" : "") . ".";
    
    registrarAuditoriaCompleta(
        $db,
        $creadorId,
        $accion,
        'tareas',
        $tareaId,
        $deviceInfo
    );

    // ============================================
    // NOTIFICACIONES
    // ============================================
    
    // ✅ CORRECCIÓN IMPORTANTE: Preparamos la consulta AQUÍ, fuera de los IFs.
    $queryNotif = "INSERT INTO notificaciones (
                      user_id, mensaje, tipo, asunto, url,
                      fecha_envio, created_at, updated_at
                   ) VALUES (
                      :user_id, :mensaje, 'tarea', :asunto, :url,
                      NOW(), NOW(), NOW()
                   )";
    $stmtNotif = $db->prepare($queryNotif);

    // 1. Notificar al asignado (si existe y no es el mismo creador)
    if ($asignadoId && $asignadoId != $creadorId) {
        $stmtNotif->execute([
            ':user_id' => $asignadoId,
            ':mensaje' => "Se ha creado la tarea '{$titulo}' en el proyecto '{$proyecto['nombre']}'.",
            ':asunto' => 'Nueva tarea creada',
            ':url' => "http://127.0.0.1:8000/proyectos/{$proyectoId}"
        ]);
    }

    // 2. Notificar a otros stakeholders del proyecto
    $queryStakeholders = "SELECT DISTINCT user_id 
                          FROM proyectos_usuarios 
                          WHERE proyecto_id = :proyecto_id 
                          AND user_id != :creador_id
                          AND eliminado = 0";
    
    $stmtStakeholders = $db->prepare($queryStakeholders);
    $stmtStakeholders->execute([
        ':proyecto_id' => $proyectoId,
        ':creador_id' => $creadorId
    ]);

    while ($row = $stmtStakeholders->fetch(PDO::FETCH_ASSOC)) {
        // Evitar doble notificación si el stakeholder es también el asignado
        if ($row['user_id'] != $asignadoId) {
            $stmtNotif->execute([
                ':user_id' => $row['user_id'],
                ':mensaje' => "Se ha creado la tarea '{$titulo}' en el proyecto '{$proyecto['nombre']}'.",
                ':asunto' => 'Nueva tarea creada',
                ':url' => "http://127.0.0.1:8000/proyectos/{$proyectoId}"
            ]);
        }
    }

    $db->commit();

    // ============================================
    // RESPUESTA
    // ============================================
    http_response_code(201);
    echo json_encode([
        "success" => true,
        "message" => "Tarea creada exitosamente",
        "data" => [
            "id" => $tareaId,
            "titulo" => $titulo,
            "proyecto_nombre" => $proyecto['nombre']
        ]
    ], JSON_UNESCAPED_UNICODE);

} catch (Exception $e) {
    $db->rollBack();
    http_response_code(500);
    echo json_encode([
        "success" => false,
        "message" => "Error al crear tarea: " . $e->getMessage()
    ]);
}
?>