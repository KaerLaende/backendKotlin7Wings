CREATE TABLE author (
                        id SERIAL PRIMARY KEY,
                        fio VARCHAR(255),
                        created_at TIMESTAMP
);

CREATE TABLE budget (
                        id SERIAL PRIMARY KEY,
                        year INT NOT NULL,
                        month INT NOT NULL,
                        amount INT NOT NULL,
                        type TEXT NOT NULL,
                        author INT REFERENCES author(id)
);