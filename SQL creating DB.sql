-- ENUM types for question difficulty and type
CREATE TYPE Difficulty_Enum AS ENUM ('easy', 'medium', 'hard');
CREATE TYPE Question_Type_Enum AS ENUM ('american', 'open');
CREATE TYPE Exam_Type AS ENUM ('manual', 'automatic');

-- Repository table
CREATE TABLE Repository (
    Id SERIAL PRIMARY KEY,
    Name VARCHAR(255) NOT NULL UNIQUE
);

-- Question table with manually controlled Ids
CREATE TABLE Question (
    Id INT NOT NULL,
    Difficulty Difficulty_Enum NOT NULL,
    Type Question_Type_Enum NOT NULL,
    Repository_id INT NOT NULL,
    Text TEXT NOT NULL,
    PRIMARY KEY (Id, Repository_id), -- Composite primary key
    FOREIGN KEY (Repository_id) REFERENCES Repository(Id)
);

-- Answer table with manually controlled Ids
CREATE TABLE Answer (
    Id INT NOT NULL,
    Text TEXT NOT NULL,
    Repository_id INT NOT NULL,
    PRIMARY KEY (Id, Repository_id), -- Composite primary key
    FOREIGN KEY (Repository_id) REFERENCES Repository(Id)
);

CREATE TABLE Open_Question (
    Id INT NOT NULL,
    Repository_id INT NOT NULL,
    Answer_id INT,
    PRIMARY KEY (Id, Repository_id),
    FOREIGN KEY (Id, Repository_id) REFERENCES Question(Id, Repository_id) ON DELETE CASCADE,
    FOREIGN KEY (Answer_id, Repository_id) REFERENCES Answer(Id, Repository_id) -- Corrected composite foreign key
);


CREATE TABLE American_Question (
    Id INT NOT NULL,
    Repository_id INT NOT NULL,
    Answer_Count INT DEFAULT 0,
    PRIMARY KEY (Id, Repository_id),
    FOREIGN KEY (Id, Repository_id) REFERENCES Question(Id, Repository_id) ON DELETE CASCADE
);

CREATE TABLE American_Question_Answer (
    American_question_id INT NOT NULL,
    Repository_id INT NOT NULL,
    Answer_id INT NOT NULL,
    Is_correct BOOLEAN NOT NULL,
    PRIMARY KEY (American_question_id, Answer_id, Repository_id), -- Composite primary key
    FOREIGN KEY (American_question_id, Repository_id) REFERENCES American_Question(Id, Repository_id) ON DELETE CASCADE,
    FOREIGN KEY (Answer_id, Repository_id) REFERENCES Answer(Id, Repository_id) ON DELETE CASCADE -- Composite foreign key
);


CREATE TABLE Exam (
    id SERIAL PRIMARY KEY,
    repository_id INT REFERENCES Repository(id),
    type Exam_Type,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE Exam_Question (
    exam_id INT REFERENCES Exam(id),
    question_id INT NOT NULL,
    repository_id INT NOT NULL,
    position INT,
    PRIMARY KEY (exam_id, question_id, repository_id),
    FOREIGN KEY (question_id, repository_id) REFERENCES Question(Id, Repository_id)
);

CREATE TABLE Exam_Question_Answer (
    exam_id INT REFERENCES Exam(id),
    question_id INT NOT NULL,
    repository_id INT NOT NULL,
    answer_id INT NOT NULL,
    PRIMARY KEY (exam_id, question_id, repository_id, answer_id),
    FOREIGN KEY (question_id, repository_id) REFERENCES Question(Id, Repository_id),
    FOREIGN KEY (answer_id, repository_id) REFERENCES Answer(Id, Repository_id) -- Corrected composite foreign key
);



-- Trigger function to calculate the next available Question Id per repository
CREATE OR REPLACE FUNCTION calculate_question_id()
RETURNS TRIGGER AS $$
BEGIN
    -- Find the maximum Id for the repository and increment by 1
    SELECT COALESCE(MAX(Id), 0) + 1 INTO NEW.Id
    FROM Question
    WHERE Repository_id = NEW.Repository_id;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger function to calculate the next available Answer Id per repository
CREATE OR REPLACE FUNCTION calculate_answer_id()
RETURNS TRIGGER AS $$
BEGIN
    -- Find the maximum Id for the repository and increment by 1
    SELECT COALESCE(MAX(Id), 0) + 1 INTO NEW.Id
    FROM Answer
    WHERE Repository_id = NEW.Repository_id;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for Question table
CREATE TRIGGER trigger_calculate_question_id
BEFORE INSERT ON Question
FOR EACH ROW
EXECUTE FUNCTION calculate_question_id();

-- Create trigger for Answer table
CREATE TRIGGER trigger_calculate_answer_id
BEFORE INSERT ON Answer
FOR EACH ROW
EXECUTE FUNCTION calculate_answer_id();

CREATE OR REPLACE FUNCTION increment_answer_count()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE American_Question
    SET Answer_Count = Answer_Count + 1
    WHERE Id = NEW.American_question_id AND Repository_id = NEW.Repository_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION decrement_answer_count()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE American_Question
    SET Answer_Count = Answer_Count - 1
    WHERE Id = OLD.American_question_id AND Repository_id = OLD.Repository_id;
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION delete_american_question_answers()
RETURNS TRIGGER AS $$
BEGIN
    DELETE FROM American_Question_Answer
    WHERE American_question_id = OLD.Id AND Repository_id = OLD.Repository_id;
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER after_insert_american_question_answer
AFTER INSERT ON American_Question_Answer
FOR EACH ROW
EXECUTE FUNCTION increment_answer_count();

CREATE TRIGGER after_delete_american_question_answer
AFTER DELETE ON American_Question_Answer
FOR EACH ROW
EXECUTE FUNCTION decrement_answer_count();

CREATE TRIGGER trigger_delete_american_question_answers
AFTER DELETE ON American_Question
FOR EACH ROW
EXECUTE FUNCTION delete_american_question_answers();
