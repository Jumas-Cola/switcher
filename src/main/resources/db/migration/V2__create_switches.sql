CREATE TABLE switches
(
  id          UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
  user_id     UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
  name        VARCHAR(100) NOT NULL,
  type        VARCHAR(10)  NOT NULL CHECK (type IN ('SWITCH', 'BUTTON')),
  state       varchar(10)  NOT NULL CHECK (state IN ('ON', 'OFF')),
  toggled_at  TIMESTAMP, -- дата последнего включения
  public_code UUID         NOT NULL,
  created_at  TIMESTAMP    NOT NULL DEFAULT now()
);
