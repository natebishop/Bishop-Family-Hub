CREATE TABLE family (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    username VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) DEFAULT now(),
    updated_at TIMESTAMP(6) DEFAULT now()
);

CREATE TABLE family_member (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id UUID NOT NULL REFERENCES family(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
--    enum
    color VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(255),
    email VARCHAR(255)
);

CREATE TABLE calendar_event (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    date DATE NOT NULL,
    is_all_day BOOLEAN NOT NULL DEFAULT FALSE,
    location VARCHAR(255),
    member_id UUID NOT NULL REFERENCES family_member(id) ON DELETE CASCADE,
    family_id UUID NOT NULL REFERENCES family(id) ON DELETE CASCADE
);