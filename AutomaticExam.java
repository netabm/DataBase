package ItayNetaDatabase;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AutomaticExam implements Examable {
	int amountOfQuestionsInExam;

	public AutomaticExam(int amountOfQuestionsInExam) {
		this.amountOfQuestionsInExam = amountOfQuestionsInExam;
	}

	public int createExam(Repository repository) throws IOException {
	    Connection conn = repository.getConnection();
	    int examId = 0;
	    try {
	        // Check if there are enough valid questions in the repository
	        if (!areValidQuestionsAvailable(conn, repository.getId(), this.amountOfQuestionsInExam)) {
	            throw new IllegalArgumentException("Not enough valid questions in the repository to create an exam.");
	        }

	        // Insert a new exam into the Exam table
	        String insertExamQuery = "INSERT INTO Exam (repository_id, type) VALUES (?, 'automatic') RETURNING id";
	        try (PreparedStatement insertExamStmt = conn.prepareStatement(insertExamQuery)) {
	            insertExamStmt.setInt(1, repository.getId());
	            try (ResultSet examResult = insertExamStmt.executeQuery()) {
	                if (examResult.next()) {
	                    examId = examResult.getInt(1);
	                }
	            }
	        }

	        int questionsAdded = 0;

	        while (questionsAdded < this.amountOfQuestionsInExam) {
	            // Select a random question from the repository that hasn't been added to the exam
	            String selectQuestionsQuery = """
	                SELECT q.Id, q.Type
	                FROM Question q
	                WHERE q.Repository_id = ?
	                  AND NOT EXISTS (
	                    SELECT 1
	                    FROM Exam_Question eq
	                    WHERE eq.exam_id = ? AND eq.question_id = q.id
	                )
	                ORDER BY RANDOM() LIMIT 1
	            """;
	            try (PreparedStatement selectQuestionsStmt = conn.prepareStatement(selectQuestionsQuery)) {
	                selectQuestionsStmt.setInt(1, repository.getId());
	                selectQuestionsStmt.setInt(2, examId);
	                try (ResultSet questionsResultSet = selectQuestionsStmt.executeQuery()) {
	                    if (questionsResultSet.next()) {
	                        int questionId = questionsResultSet.getInt("Id");
	                        String questionType = questionsResultSet.getString("Type");

	                        boolean questionAdded = false;

	                        if ("american".equals(questionType)) {
	                            // Check the number of answers for the American question
	                            String checkAnswerCountQuery = """
	                                SELECT Answer_Count
	                                FROM American_Question
	                                WHERE Id = ? AND Repository_id = ?
	                            """;
	                            try (PreparedStatement checkAnswerCountStmt = conn.prepareStatement(checkAnswerCountQuery)) {
	                                checkAnswerCountStmt.setInt(1, questionId);
	                                checkAnswerCountStmt.setInt(2, repository.getId());
	                                try (ResultSet answerCountResultSet = checkAnswerCountStmt.executeQuery()) {
	                                    if (answerCountResultSet.next()) {
	                                        int answerCount = answerCountResultSet.getInt("Answer_Count");

	                                        if (answerCount < 4) {
	                                            continue; // Skip this question and generate another
	                                        }

	                                        // Automatically assign 4 random answers for American questions
	                                        assignAnswersToAmericanQuestion(conn, examId, questionId, repository.getId());
	                                        questionAdded = true;
	                                    }
	                                }
	                            }
	                        } else if ("open".equals(questionType)) {
	                            // Automatically assign an answer for Open questions
	                            assignOpenQuestionAnswer(conn, examId, questionId, repository.getId());
	                            questionAdded = true;
	                        }

	                        if (questionAdded) {
	                            // Insert question into the Exam_Question table
	                            String insertQuestionQuery = """
	                                INSERT INTO Exam_Question (exam_id, question_id, repository_id, position)
	                                VALUES (?, ?, ?, ?)
	                            """;
	                            try (PreparedStatement insertQuestionStmt = conn.prepareStatement(insertQuestionQuery)) {
	                                insertQuestionStmt.setInt(1, examId);
	                                insertQuestionStmt.setInt(2, questionId);
	                                insertQuestionStmt.setInt(3, repository.getId());
	                                insertQuestionStmt.setInt(4, ++questionsAdded); // Adjust the position logic as needed
	                                insertQuestionStmt.executeUpdate();
	                            }
	                        }
	                    }
	                }
	            }
	        }

	    } catch (IllegalArgumentException e) {
	        throw new IOException(e);
	    } catch (SQLException e) {
	        e.printStackTrace();
	        throw new IOException("Error creating the exam.", e);
	    }

	    return examId;
	}


	private boolean areValidQuestionsAvailable(Connection conn, int repositoryId, int requiredAmount)
			throws SQLException {
		// Count valid American questions with at least 4 answers
		String validAmericanCountQuery = """
				    SELECT COUNT(*)
				    FROM American_Question a
				    JOIN Question q ON a.Id = q.id
				    WHERE q.repository_id = ?
				      AND a.Answer_Count >= 4
				""";
		try (PreparedStatement validAmericanCountStmt = conn.prepareStatement(validAmericanCountQuery)) {
	        validAmericanCountStmt.setInt(1, repositoryId);
	        int validAmericanCount = 0;
	        try (ResultSet validAmericanCountResult = validAmericanCountStmt.executeQuery()) {
	            if (validAmericanCountResult.next()) {
	                validAmericanCount = validAmericanCountResult.getInt(1);
	            }
	        }

	        // Count Open questions
	        String openQuestionCountQuery = "SELECT COUNT(*) FROM Question WHERE repository_id = ? AND type = 'open'";
	        try (PreparedStatement openQuestionCountStmt = conn.prepareStatement(openQuestionCountQuery)) {
	            openQuestionCountStmt.setInt(1, repositoryId);
	            int openQuestionCount = 0;
	            try (ResultSet openQuestionCountResult = openQuestionCountStmt.executeQuery()) {
	                if (openQuestionCountResult.next()) {
	                    openQuestionCount = openQuestionCountResult.getInt(1);
	                }
	            }

	            // Check if the total count of valid questions meets the requirement
	            return (validAmericanCount + openQuestionCount) >= requiredAmount;
	        }
		}
	}

	private void assignOpenQuestionAnswer(Connection conn, int examId, int questionId, int repositoryId) throws SQLException {
	    // Query to select the answer for the open question
	    String selectAnswerQuery = """
	        SELECT Answer_id
	        FROM Open_Question
	        WHERE Id = ? AND Repository_id = ?
	    """;
	    try (PreparedStatement selectAnswerStmt = conn.prepareStatement(selectAnswerQuery)) {
	        selectAnswerStmt.setInt(1, questionId);
	        selectAnswerStmt.setInt(2, repositoryId);
	        try (ResultSet answerResultSet = selectAnswerStmt.executeQuery()) {
	            if (answerResultSet.next()) {
	                int answerId = answerResultSet.getInt("Answer_id");

	                // Insert answer into the Exam_Question_Answer table
	                String insertAnswerQuery = """
	                    INSERT INTO Exam_Question_Answer (exam_id, question_id, repository_id, answer_id)
	                    VALUES (?, ?, ?, ?)
	                """;
	                try (PreparedStatement insertAnswerStmt = conn.prepareStatement(insertAnswerQuery)) {
	                    insertAnswerStmt.setInt(1, examId);
	                    insertAnswerStmt.setInt(2, questionId);
	                    insertAnswerStmt.setInt(3, repositoryId);
	                    insertAnswerStmt.setInt(4, answerId);
	                    insertAnswerStmt.executeUpdate();
	                }
	            } else {
	                throw new SQLException("No answer found for open question ID " + questionId);
	            }
	        }
	    }
	}
	

	private void assignAnswersToAmericanQuestion(Connection conn, int examId, int questionId, int repositoryId) throws SQLException {
	    // Fetch 4 random answers for the question
	    String selectRandomAnswersQuery = """
	        SELECT qa.answer_id
	        FROM American_Question_Answer qa
	        WHERE qa.american_question_id = ? AND qa.repository_id = ?
	        ORDER BY RANDOM() LIMIT 4;
	    """;
	    
	    try (PreparedStatement selectRandomAnswersStmt = conn.prepareStatement(selectRandomAnswersQuery)) {
	        selectRandomAnswersStmt.setInt(1, questionId);
	        selectRandomAnswersStmt.setInt(2, repositoryId);
	        
	        try (ResultSet answersResultSet = selectRandomAnswersStmt.executeQuery()) {
	            String insertAnswerQuery = """
	                INSERT INTO Exam_Question_Answer (exam_id, question_id, repository_id, answer_id)
	                VALUES (?, ?, ?, ?)
	            """;
	            
	            try (PreparedStatement insertAnswerStmt = conn.prepareStatement(insertAnswerQuery)) {
	                // Insert each selected answer into the Exam_Question_Answer table
	                while (answersResultSet.next()) {
	                    int answerId = answersResultSet.getInt("answer_id");
	                    
	                    // Insert into Exam_Question_Answer
	                    insertAnswerStmt.setInt(1, examId);
	                    insertAnswerStmt.setInt(2, questionId);
	                    insertAnswerStmt.setInt(3, repositoryId);
	                    insertAnswerStmt.setInt(4, answerId);
	                    insertAnswerStmt.executeUpdate();
	                }
	            }
	        }
	    }
	}

}
