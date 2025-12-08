<?php
// ============================================
// CREAR NUEVO PROYECTO
// ============================================
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST");

include_once '../db_config/database.php';
include_once '../db_config/audit_helper.php';

$database = new Database();
$db = $database->getConnection();

// ============================================
// AUTENTICACIÓN
// ============================================
$headers = getallheaders();
$token = isset($headers['Authorization']) ? str_replace('Bearer ', '', $headers['Authorization']) : null;

if (!$token) {
    http_response_code(401);
    echo json_encode(["success" => false, "message" => "Token no proporcionado"]);
    exit();
}

$usuario = obtenerUsuarioDesdeToken($db, $token);
if (!$usuario) {
    http_response_code(401);
    echo json_encode(["success" => false, "message" => "Token inválido"]);
    exit();
}

// ============================================
// VALIDAR PERMISOS
// ============================================
$rol_permitido = in_array(strtolower($usuario['rol']), ['admin', 'arquitecto']);
if (!$rol_permitido) {
    http_response_code(403);
    echo json_encode(["success" => false, "message" => "No tienes permisos para crear proyectos"]);
    exit();
}

// ============================================
// RECIBIR DATOS
// ============================================
$data = json_decode(file_get_contents("php://input"));

$nombre = isset($data->nombre) ? trim($data->nombre) : null;
$descripcion = isset($data->descripcion) ? trim($data->descripcion) : null;
$estado = isset($data->estado) ? trim($data->estado) : 'activo';
$fecha_inicio = isset($data->fecha_inicio) ? trim($data->fecha_inicio) : null;
$fecha_fin = isset($data->fecha_fin) ? trim($data->fecha_fin) : null;
$cliente_id = isset($data->cliente_id) ? intval($data->cliente_id) : null;
$responsable_id = isset($data->responsable_id) ? intval($data->responsable_id) : null;

// Información del dispositivo (auditoría)
$device_info = extraerInfoDispositivo($data);

// ============================================
// VALIDACIONES
// ============================================
if (!$nombre || !$fecha_inicio || !$cliente_id || !$responsable_id) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "Faltan datos obligatorios"]);
    exit();
}

// Validar que cliente y responsable existan
$query_validate = "SELECT id FROM users WHERE id IN (:cliente_id, :responsable_id) AND eliminado = 0";
$stmt_validate = $db->prepare($query_validate);
$stmt_validate->execute([
    ':cliente_id' => $cliente_id,
    ':responsable_id' => $responsable_id
]);

if ($stmt_validate->rowCount() < 2) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "Cliente o responsable no válidos"]);
    exit();
}

// ============================================
// INSERTAR PROYECTO
// ============================================
try {
    $db->beginTransaction();

    $query = "INSERT INTO proyectos 
              (nombre, descripcion, estado, fecha_inicio, fecha_fin, cliente_id, responsable_id, created_at, updated_at) 
              VALUES 
              (:nombre, :descripcion, :estado, :fecha_inicio, :fecha_fin, :cliente_id, :responsable_id, NOW(), NOW())";
    
    $stmt = $db->prepare($query);
    $stmt->execute([
        ':nombre' => $nombre,
        ':descripcion' => $descripcion,
        ':estado' => $estado,
        ':fecha_inicio' => $fecha_inicio,
        ':fecha_fin' => $fecha_fin,
        ':cliente_id' => $cliente_id,
        ':responsable_id' => $responsable_id
    ]);
    
    $proyecto_id = $db->lastInsertId();

    // ============================================
    // ASIGNAR CLIENTE Y RESPONSABLE
    // ============================================
    $query_cliente = "INSERT INTO proyectos_usuarios (proyecto_id, user_id, rol_en_proyecto) 
                      VALUES (:proyecto_id, :cliente_id, 'cliente')";
    $stmt_cliente = $db->prepare($query_cliente);
    $stmt_cliente->execute([
        ':proyecto_id' => $proyecto_id,
        ':cliente_id' => $cliente_id
    ]);

    $query_responsable = "INSERT INTO proyectos_usuarios (proyecto_id, user_id, rol_en_proyecto) 
                          VALUES (:proyecto_id, :responsable_id, 'responsable')";
    $stmt_responsable = $db->prepare($query_responsable);
    $stmt_responsable->execute([
        ':proyecto_id' => $proyecto_id,
        ':responsable_id' => $responsable_id
    ]);

    // ============================================
    // CREAR HITO INICIAL
    // ============================================
    $query_hito = "INSERT INTO hitos (proyecto_id, nombre, fecha_hito, descripcion, estado, encargado_id, created_at, updated_at)
                   VALUES (:proyecto_id, 'Proyecto iniciado', :fecha_inicio, 'Creación del proyecto', 'Completado', :responsable_id, NOW(), NOW())";
    $stmt_hito = $db->prepare($query_hito);
    $stmt_hito->execute([
        ':proyecto_id' => $proyecto_id,
        ':fecha_inicio' => $fecha_inicio,
        ':responsable_id' => $responsable_id
    ]);

    // ============================================
    // REGISTRAR AUDITORÍA
    // ============================================
    registrarAuditoriaCompleta(
        $db,
        $usuario['id'],
        "Creó el proyecto '$nombre' con cliente #$cliente_id y responsable #$responsable_id.",
        'proyectos',
        $proyecto_id,
        $device_info
    );

    registrarAuditoriaCompleta(
        $db,
        $usuario['id'],
        "Asignó cliente y responsable al proyecto '$nombre'.",
        'proyectos_usuarios',
        $proyecto_id,
        $device_info
    );

    // ============================================
    // ENVIAR NOTIFICACIONES
    // ============================================
    $query_notif_responsable = "INSERT INTO notificaciones (user_id, mensaje, tipo, asunto, url, created_at, updated_at)
                                VALUES (:user_id, :mensaje, 'proyecto', 'Asignación de proyecto', :url, NOW(), NOW())";
    $stmt_notif_responsable = $db->prepare($query_notif_responsable);
    $stmt_notif_responsable->execute([
        ':user_id' => $responsable_id,
        ':mensaje' => "Se te ha asignado el proyecto: $nombre",
        ':url' => "http://127.0.0.1:8000/proyectos/$proyecto_id"
    ]);

    $query_notif_cliente = "INSERT INTO notificaciones (user_id, mensaje, tipo, asunto, url, created_at, updated_at)
                            VALUES (:user_id, :mensaje, 'proyecto', 'Proyecto creado', :url, NOW(), NOW())";
    $stmt_notif_cliente = $db->prepare($query_notif_cliente);
    $stmt_notif_cliente->execute([
        ':user_id' => $cliente_id,
        ':mensaje' => "Tu proyecto '$nombre' ha sido creado correctamente.",
        ':url' => "http://127.0.0.1:8000/proyectos/$proyecto_id"
    ]);

    $db->commit();

    // ============================================
    // RESPUESTA EXITOSA
    // ============================================
    echo json_encode([
        "success" => true,
        "message" => "Proyecto creado exitosamente",
        "data" => [
            "id" => intval($proyecto_id),
            "nombre" => $nombre,
            "descripcion" => $descripcion,
            "estado" => $estado,
            "fecha_inicio" => $fecha_inicio,
            "fecha_fin" => $fecha_fin,
            "cliente_id" => intval($cliente_id),
            "responsable_id" => intval($responsable_id)
        ]
    ]);
    
} catch (Exception $e) {
    $db->rollBack();
    http_response_code(500);
    echo json_encode([
        "success" => false,
        "message" => "Error al crear proyecto: " . $e->getMessage()
    ]);
}
?>