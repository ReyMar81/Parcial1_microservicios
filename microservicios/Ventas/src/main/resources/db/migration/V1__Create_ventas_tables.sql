CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE ventas (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    fecha DATE NOT NULL,
    cliente_id UUID NOT NULL,
    estado TEXT NOT NULL CHECK (estado IN ('CREADA', 'CONFIRMADA', 'ANULADA')),
    total NUMERIC(12,2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE detalleventa (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    venta_id UUID NOT NULL,
    producto_id UUID NOT NULL,
    cantidad INTEGER NOT NULL,
    precio_unit NUMERIC(12,2) NOT NULL,
    FOREIGN KEY (venta_id) REFERENCES ventas(id) ON DELETE CASCADE
);

CREATE TABLE notas_venta (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    venta_id UUID NOT NULL UNIQUE,
    numero_nota TEXT NOT NULL UNIQUE,
    fecha_emision TIMESTAMP DEFAULT NOW(),
    pdf_path TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    FOREIGN KEY (venta_id) REFERENCES ventas(id) ON DELETE CASCADE
);

CREATE INDEX idx_ventas_cliente_id ON ventas(cliente_id);
CREATE INDEX idx_ventas_fecha ON ventas(fecha);
CREATE INDEX idx_ventas_estado ON ventas(estado);
CREATE INDEX idx_notas_venta_numero ON notas_venta(numero_nota);
CREATE INDEX idx_notas_venta_fecha ON notas_venta(fecha_emision);
CREATE INDEX idx_detalleventa_venta_id ON detalleventa(venta_id);
CREATE INDEX idx_detalleventa_producto_id ON detalleventa(producto_id);
