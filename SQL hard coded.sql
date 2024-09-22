-- Insert Repositories
INSERT INTO Repository (Name) VALUES ('History of Israel');
INSERT INTO Repository (Name) VALUES ('Mathematics');
INSERT INTO Repository (Name) VALUES ('Computer Science');

-- Insert Questions and Answers for 'History of Israel' repository

-- Open Question (History)
INSERT INTO Question (Difficulty, Type, Repository_id, Text) 
VALUES ('medium', 'open', 1, 'Who was the first Prime Minister of Israel?');
INSERT INTO Answer (Text, Repository_id) VALUES ('David Ben-Gurion', 1);
INSERT INTO Open_Question (Id, Repository_id, Answer_id) VALUES (1, 1, 1); -- Link open question with answer

-- American Question (History)
INSERT INTO Question (Difficulty, Type, Repository_id, Text) 
VALUES ('easy', 'american', 1, 'In which year was the state of Israel established?');
INSERT INTO American_Question (Id, Repository_id) VALUES (2, 1);

-- Answers for American Question
INSERT INTO Answer (Text, Repository_id) VALUES ('1948', 1);  -- Correct
INSERT INTO Answer (Text, Repository_id) VALUES ('1950', 1);
INSERT INTO Answer (Text, Repository_id) VALUES ('1967', 1);
INSERT INTO Answer (Text, Repository_id) VALUES ('1945', 1);

-- Link answers to the American question
INSERT INTO American_Question_Answer (American_question_id, Repository_id, Answer_id, Is_correct) 
VALUES (2, 1, 2, true);  -- Correct answer
INSERT INTO American_Question_Answer (American_question_id, Repository_id, Answer_id, Is_correct) 
VALUES (2, 1, 3, false);
INSERT INTO American_Question_Answer (American_question_id, Repository_id, Answer_id, Is_correct) 
VALUES (2, 1, 4, false);
INSERT INTO American_Question_Answer (American_question_id, Repository_id, Answer_id, Is_correct) 
VALUES (2, 1, 5, false);

-- Insert Questions and Answers for 'Mathematics' repository

-- Open Question (Math)
INSERT INTO Question (Difficulty, Type, Repository_id, Text) 
VALUES ('hard', 'open', 2, 'What is the derivative of x^2?');
INSERT INTO Answer (Text, Repository_id) VALUES ('2x', 2);
INSERT INTO Open_Question (Id, Repository_id, Answer_id) VALUES (1, 2, 1); -- Link open question with answer

-- American Question (Math)
INSERT INTO Question (Difficulty, Type, Repository_id, Text) 
VALUES ('easy', 'american', 2, 'What is 2 + 2?');
INSERT INTO American_Question (Id, Repository_id) VALUES (2, 2);

-- Answers for American Question
INSERT INTO Answer (Text, Repository_id) VALUES ('3', 2);
INSERT INTO Answer (Text, Repository_id) VALUES ('4', 2);  -- Correct
INSERT INTO Answer (Text, Repository_id) VALUES ('5', 2);
INSERT INTO Answer (Text, Repository_id) VALUES ('6', 2);

-- Link answers to the American question
INSERT INTO American_Question_Answer (American_question_id, Repository_id, Answer_id, Is_correct) 
VALUES (2, 2, 2, false);
INSERT INTO American_Question_Answer (American_question_id, Repository_id, Answer_id, Is_correct) 
VALUES (2, 2, 3, true);  -- Correct answer
INSERT INTO American_Question_Answer (American_question_id, Repository_id, Answer_id, Is_correct) 
VALUES (2, 2, 4, false);
INSERT INTO American_Question_Answer (American_question_id, Repository_id, Answer_id, Is_correct) 
VALUES (2, 2, 5, false);

-- Insert Questions and Answers for 'Computer Science' repository

-- Open Question (Computer Science)
INSERT INTO Question (Difficulty, Type, Repository_id, Text) 
VALUES ('medium', 'open', 3, 'What is the time complexity of binary search?');
INSERT INTO Answer (Text, Repository_id) VALUES ('O(log n)', 3);
INSERT INTO Open_Question (Id, Repository_id, Answer_id) VALUES (1, 3, 1); -- Link open question with answer

-- American Question (Computer Science)
INSERT INTO Question (Difficulty, Type, Repository_id, Text) 
VALUES ('easy', 'american', 3, 'Which language is known as the backbone of web development?');
INSERT INTO American_Question (Id, Repository_id) VALUES (2, 3);

-- Answers for American Question
INSERT INTO Answer (Text, Repository_id) VALUES ('Python', 3);
INSERT INTO Answer (Text, Repository_id) VALUES ('JavaScript', 3);  -- Correct
INSERT INTO Answer (Text, Repository_id) VALUES ('C++', 3);
INSERT INTO Answer (Text, Repository_id) VALUES ('Java', 3);

-- Link answers to the American question
INSERT INTO American_Question_Answer (American_question_id, Repository_id, Answer_id, Is_correct) 
VALUES (2, 3, 2, false);
INSERT INTO American_Question_Answer (American_question_id, Repository_id, Answer_id, Is_correct) 
VALUES (2, 3, 3, true);  -- Correct answer
INSERT INTO American_Question_Answer (American_question_id, Repository_id, Answer_id, Is_correct) 
VALUES (2, 3, 4, false);
INSERT INTO American_Question_Answer (American_question_id, Repository_id, Answer_id, Is_correct) 
VALUES (2, 3, 5, false);
