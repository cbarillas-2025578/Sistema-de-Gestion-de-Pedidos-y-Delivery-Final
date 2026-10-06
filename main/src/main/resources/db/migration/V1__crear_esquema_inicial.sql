-- =====================================================================
-- V1: Esquema inicial del sistema de comercio electrónico y delivery
-- Fuente única de verdad del esquema (Hibernate opera con ddl-auto=validate)
-- =====================================================================

-- ---------------------------------------------------------------------
-- usuarios
-- ---------------------------------------------------------------------
CREATE TABLE usuarios (
    id              BIGSERIAL PRIMARY KEY,
    nombre          VARCHAR(100)  NOT NULL,
    direccion       VARCHAR(255),
    telefono        VARCHAR(20),
    email           VARCHAR(100)  NOT NULL,
    password        VARCHAR(255)  NOT NULL,
    rol             VARCHAR(20)   NOT NULL CHECK (rol IN ('ADMIN', 'REPARTIDOR', 'CLIENTE')),
    activo          BOOLEAN       NOT NULL DEFAULT TRUE,
    fecha_creacion  TIMESTAMP     NOT NULL DEFAULT now(),
    CONSTRAINT uk_usuarios_email UNIQUE (email)
);

CREATE INDEX idx_usuarios_rol ON usuarios (rol);

-- ---------------------------------------------------------------------
-- categorías globales de productos
-- ---------------------------------------------------------------------
CREATE TABLE categorias (
    id          BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(50) NOT NULL,
    descripcion VARCHAR(255),
    activo      BOOLEAN     NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_categorias_nombre UNIQUE (nombre)
);

-- ---------------------------------------------------------------------
-- comercios
-- ---------------------------------------------------------------------
CREATE TABLE comercios (
    id              BIGSERIAL PRIMARY KEY,
    nombre          VARCHAR(100) NOT NULL,
    categoria       VARCHAR(30)  NOT NULL CHECK (categoria IN ('RESTAURANTE', 'SUPERMERCADO', 'FARMACIA')),
    direccion       VARCHAR(255) NOT NULL,
    abierto         BOOLEAN      NOT NULL DEFAULT TRUE,
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion  TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_comercios_activo_abierto ON comercios (activo, abierto);

-- ---------------------------------------------------------------------
-- productos
-- ---------------------------------------------------------------------
CREATE TABLE productos (
    id                BIGSERIAL PRIMARY KEY,
    comercio_id       BIGINT        NOT NULL REFERENCES comercios (id),
    categoria_id      BIGINT        REFERENCES categorias (id) ON DELETE SET NULL,
    nombre            VARCHAR(100)  NOT NULL,
    descripcion       VARCHAR(255),
    precio            NUMERIC(10, 2) NOT NULL CHECK (precio > 0),
    stock             INTEGER       NOT NULL DEFAULT 0 CHECK (stock >= 0),
    disponible        BOOLEAN       NOT NULL DEFAULT TRUE,
    fecha_creacion    TIMESTAMP     NOT NULL DEFAULT now(),
    fecha_actualizacion TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE INDEX idx_productos_comercio ON productos (comercio_id);
CREATE INDEX idx_productos_categoria ON productos (categoria_id);
CREATE INDEX idx_productos_disponibilidad ON productos (comercio_id, disponible, stock);

-- ---------------------------------------------------------------------
-- pedidos (un pedido = un único comercio)
-- ---------------------------------------------------------------------
CREATE TABLE pedidos (
    id              BIGSERIAL PRIMARY KEY,
    cliente_id      BIGINT        NOT NULL REFERENCES usuarios (id),
    repartidor_id   BIGINT        REFERENCES usuarios (id),
    comercio_id     BIGINT        NOT NULL REFERENCES comercios (id),
    fecha_pedido    TIMESTAMP     NOT NULL DEFAULT now(),
    costo_envio     NUMERIC(10, 2) NOT NULL CHECK (costo_envio >= 0),
    monto_total     NUMERIC(10, 2) NOT NULL CHECK (monto_total >= 0),
    estado          VARCHAR(20)   NOT NULL CHECK (estado IN
                        ('PENDIENTE', 'EN_PREPARACION', 'EN_CAMINO', 'ENTREGADO', 'CANCELADO')),
    version         BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT fk_pedidos_cliente FOREIGN KEY (cliente_id) REFERENCES usuarios (id),
    CONSTRAINT fk_pedidos_repartidor FOREIGN KEY (repartidor_id) REFERENCES usuarios (id)
);

CREATE INDEX idx_pedidos_cliente ON pedidos (cliente_id, fecha_pedido DESC);
CREATE INDEX idx_pedidos_repartidor ON pedidos (repartidor_id, fecha_pedido DESC);
CREATE INDEX idx_pedidos_estado ON pedidos (estado);
-- Consulta de pedidos elegibles para reparto (evita index scan sobre toda la tabla)
CREATE UNIQUE INDEX idx_pedidos_disponibles ON pedidos (id) WHERE estado = 'EN_PREPARACION' AND repartidor_id IS NULL;
CREATE INDEX idx_pedidos_fecha ON pedidos (fecha_pedido DESC);

-- ---------------------------------------------------------------------
-- detalles de pedido (precios históricos)
-- ---------------------------------------------------------------------
CREATE TABLE detalles_pedido (
    id              BIGSERIAL PRIMARY KEY,
    pedido_id       BIGINT        NOT NULL REFERENCES pedidos (id) ON DELETE CASCADE,
    producto_id     BIGINT        NOT NULL REFERENCES productos (id),
    cantidad        INTEGER       NOT NULL CHECK (cantidad > 0),
    precio_unitario NUMERIC(10, 2) NOT NULL CHECK (precio_unitario > 0),
    subtotal        NUMERIC(10, 2) NOT NULL CHECK (subtotal >= 0)
);

CREATE INDEX idx_detalles_pedido ON detalles_pedido (pedido_id);
CREATE INDEX idx_detalles_producto ON detalles_pedido (producto_id);

-- ---------------------------------------------------------------------
-- historial de estados del pedido (seguimiento)
-- ---------------------------------------------------------------------
CREATE TABLE historial_estado_pedido (
    id              BIGSERIAL PRIMARY KEY,
    pedido_id       BIGINT      NOT NULL REFERENCES pedidos (id) ON DELETE CASCADE,
    estado          VARCHAR(20) NOT NULL CHECK (estado IN
                        ('PENDIENTE', 'EN_PREPARACION', 'EN_CAMINO', 'ENTREGADO', 'CANCELADO')),
    estado_anterior VARCHAR(20) CHECK (estado_anterior IS NULL OR estado_anterior IN
                        ('PENDIENTE', 'EN_PREPARACION', 'EN_CAMINO', 'ENTREGADO', 'CANCELADO')),
    usuario         VARCHAR(100) NOT NULL,
    fecha           TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE INDEX idx_historial_pedido_fecha ON historial_estado_pedido (pedido_id, fecha);

-- ---------------------------------------------------------------------
-- auditoría de operaciones relevantes (nunca secretos)
-- ---------------------------------------------------------------------
CREATE TABLE registros_auditoria (
    id          BIGSERIAL PRIMARY KEY,
    usuario     VARCHAR(100) NOT NULL,
    accion      VARCHAR(50)  NOT NULL,
    entidad     VARCHAR(50)  NOT NULL,
    entidad_id  BIGINT,
    detalles    TEXT,
    fecha       TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_auditoria_fecha ON registros_auditoria (fecha DESC);
CREATE INDEX idx_auditoria_usuario ON registros_auditoria (usuario);
CREATE INDEX idx_auditoria_accion ON registros_auditoria (accion);
CREATE INDEX idx_auditoria_entidad ON registros_auditoria (entidad, entidad_id);
