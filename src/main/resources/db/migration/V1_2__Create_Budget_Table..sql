CREATE TABLE budget (
                        id SERIAL PRIMARY KEY,
                        year INT NOT NULL,
                        month INT NOT NULL,
                        amount INT NOT NULL,
                        type TEXT NOT NULL,
                        author INT REFERENCES author(id)
);