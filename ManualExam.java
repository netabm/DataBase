package ItayNetaDatabase;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

public class ManualExam implements Examable {
	int amountOfQuestionsInExam;
	static Scanner input = new Scanner(System.in);

	public ManualExam(int amountOfQuestionsInExam) {
		this.amountOfQuestionsInExam = amountOfQuestionsInExam;
	}

	public int createExam(Repository repository) throws IOException {
	    Connection conn = repository.getConnection();
	    int examId = 0;
	    try {
	        // Validate the number of questions requested by the user
	        if (!areValidQuestionsAvailable(conn, repository.getId(), this.amountOfQuestionsInExam)) {
	            throw new IllegalArgumentException("Not enough questions in the repository to create the exam.");
	        }

	        // Create a new exam record
	        String createExamQuery = "INSERT INTO Exam (repository_id, type) VALUES (?, 'manual') RETURNING id";
	        try (PreparedStatement createExamStmt = conn.prepareStatement(createExamQuery)) {
	            createExamStmt.setInt(1, repository.getId());
	            try (ResultSet examIdResult = createExamStmt.executeQuery()) {
	                if (examIdResult.next()) {
	                    examId = examIdResult.getInt(1);
	                }
	            }
	        }

	        // Select questions for the exam
	        for (int i = 0; i < this.amountOfQuestionsInExam; i++) {
	            System.out.println("Which of the following questions would you like to add to your test?");
	            System.out.println("So far, you have chosen " + i + " questions");

	            repository.displayAllQuestions();

	            // User selects a question
	            int chosenQuestionId = input.nextInt();

	            // Validate user input (ensure the question ID exists and is not already chosen)
	            while (!repository.questionExists(chosenQuestionId)
	                    || isQuestionAlreadyInExam(conn, examId, chosenQuestionId, repository.getId())) {
	                if (isQuestionAlreadyInExam(conn, examId, chosenQuestionId, repository.getId())) {
	                    System.out.println("This question is already in the exam. Please choose another.");
	                } else {
	                    System.out.println("Invalid input, please choose again.");
	                }
	                chosenQuestionId = input.nextInt();
	            }

	            // Insert selected question into Exam_Question
	            String insertQuestionQuery = "INSERT INTO Exam_Question (exam_id, question_id, repository_id, position) VALUES (?, ?, ?, ?)";
	            try (PreparedStatement insertQuestionStmt = conn.prepareStatement(insertQuestionQuery)) {
	                insertQuestionStmt.setInt(1, examId);
	                insertQuestionStmt.setInt(2, chosenQuestionId);
	                insertQuestionStmt.setInt(3, repository.getId());
	                insertQuestionStmt.setInt(4, i + 1); // Position in the exam
	                insertQuestionStmt.executeUpdate();
	            }

	            // Get the question type
	            String getQuestionTypeQuery = "SELECT type FROM Question WHERE id = ? AND repository_id = ?";
	            try (PreparedStatement getQuestionTypeStmt = conn.prepareStatement(getQuestionTypeQuery)) {
	                getQuestionTypeStmt.setInt(1, chosenQuestionId);
	                getQuestionTypeStmt.setInt(2, repository.getId());
	                try (ResultSet questionTypeResult = getQuestionTypeStmt.executeQuery()) {
	                    String chosenQuestionType = "";
	                    if (questionTypeResult.next()) {
	                        chosenQuestionType = questionTypeResult.getString("type");
	                    }

	                    if (chosenQuestionType.equals("american")) {
	                        // Process the selected American question
	                        processAmericanQuestion(conn, examId, chosenQuestionId, repository.getId());
	                    } else {
	                        // Process the selected Open question
	                        processOpenQuestion(conn, examId, chosenQuestionId, repository.getId());
	                    }
	                }
	            }
	        }

	    } catch (IllegalArgumentException e) {
	        throw new IOException("Invalid number of questions.", e);
	    } catch (SQLException e) {
	        e.printStackTrace();
	        throw new IOException("Error creating the exam.", e);
	    }

	    return examId;
	}


	private boolean areValidQuestionsAvailable(Connection conn, int repositoryId, int requiredAmount) throws SQLException {
	    // Query to count the total number of questions in the repository
	    String countQuestionsQuery = """
	            SELECT COUNT(*)
	            FROM Question
	            WHERE Repository_id = ?;
	            """;

	    try (PreparedStatement countQuestionsStmt = conn.prepareStatement(countQuestionsQuery)) {
	        countQuestionsStmt.setInt(1, repositoryId);

	        try (ResultSet countQuestionsResult = countQuestionsStmt.executeQuery()) {
	            int totalQuestions = 0;
	            if (countQuestionsResult.next()) {
	                totalQuestions = countQuestionsResult.getInt(1);
	            }

	            // Check if the total count of questions meets the requirement
	            return totalQuestions >= requiredAmount;
	        }
	    }
	}


	// Check if the question is already in the exam
	private boolean isQuestionAlreadyInExam(Connection conn, int examId, int questionId, int repositoryId) throws SQLException {
	    // SQL query to check if the question is already in the exam
	    String query = """
	            SELECT COUNT(*)
	            FROM Exam_Question
	            WHERE exam_id = ? AND question_id = ? AND repository_id = ?;
	            """;

	    try (PreparedStatement stmt = conn.prepareStatement(query)) {
	        stmt.setInt(1, examId);
	        stmt.setInt(2, questionId);
	        stmt.setInt(3, repositoryId);

	        try (ResultSet resultSet = stmt.executeQuery()) {
	            if (resultSet.next()) {
	                // Return true if the count is greater than 0, indicating the question is in the exam
	                return resultSet.getInt(1) > 0;
	            } else {
	                // If no results are found, return false
	                return false;
	            }
	        }
	    }
	}

	// Process an American question (user chooses answers)
	private void processAmericanQuestion(Connection conn, int examId, int questionId, int repositoryId) throws SQLException {
	    // Query answers for the chosen American question
	    String selectAnswersQuery = """
	        SELECT a.id, a.text, qa.is_correct
	        FROM American_Question_Answer qa
	        JOIN Answer a ON qa.answer_id = a.id AND qa.repository_id = a.repository_id
	        WHERE qa.american_question_id = ? AND qa.repository_id = ?
	    """;
	    try (PreparedStatement selectAnswersStmt = conn.prepareStatement(selectAnswersQuery)) {
	        selectAnswersStmt.setInt(1, questionId);
	        selectAnswersStmt.setInt(2, repositoryId);
	        
	        try (ResultSet resultSet = selectAnswersStmt.executeQuery()) {
	            // Display answers
	            System.out.println("Available answers for question ID " + questionId + ":");
	            while (resultSet.next()) {
	                int answerId = resultSet.getInt("id");
	                String answerText = resultSet.getString("text");
	                boolean isCorrect = resultSet.getBoolean("is_correct");
	                System.out.println("ID: " + answerId + ". Answer: " + answerText + ", Correct: " + isCorrect);
	            }
	        }
	    }

	    // Let user select answers for the exam
	    String insertAnswerQuery = "INSERT INTO Exam_Question_Answer (exam_id, question_id, repository_id, answer_id) VALUES (?, ?, ?, ?)";
	    try (PreparedStatement insertAnswerStmt = conn.prepareStatement(insertAnswerQuery)) {
	        System.out.println("Select answers for this question (enter -1 to finish):");
	        while (true) {
	            int chosenAnswerId = input.nextInt();
	            if (chosenAnswerId == -1) {
	                break; // Stop choosing answers
	            }

	            // Validate answer selection (ensure the answer is not already chosen)
	            while (isAnswerAlreadyInExam(conn, examId, questionId, chosenAnswerId, repositoryId)) {
	                System.out.println("This answer has already been selected. Please choose another.");
	                chosenAnswerId = input.nextInt();
	                if (chosenAnswerId == -1) {
	                    break;
	                }
	            }

	            if (chosenAnswerId != -1) {
	                insertAnswerStmt.setInt(1, examId);
	                insertAnswerStmt.setInt(2, questionId);
	                insertAnswerStmt.setInt(3, repositoryId);
	                insertAnswerStmt.setInt(4, chosenAnswerId);
	                insertAnswerStmt.executeUpdate();
	            }
	        }
	    }
	}


	// Process an Open question (user selects the single correct answer)
	private void processOpenQuestion(Connection conn, int examId, int questionId, int repositoryId) throws SQLException {
	    // Query to get the answer directly from Open_Question
	    String selectAnswerQuery = "SELECT answer_id FROM Open_Question WHERE id = ? AND repository_id = ?";
	    try (PreparedStatement selectAnswerStmt = conn.prepareStatement(selectAnswerQuery)) {
	        selectAnswerStmt.setInt(1, questionId);
	        selectAnswerStmt.setInt(2, repositoryId);
	        
	        try (ResultSet resultSet = selectAnswerStmt.executeQuery()) {
	            // There should be only one answer for open questions, so we directly add it to the exam
	            if (resultSet.next()) {
	                int answerId = resultSet.getInt("answer_id");

	                // Query to get the answer text for display purposes
	                String answerTextQuery = "SELECT text FROM Answer WHERE id = ? AND repository_id = ?";
	                try (PreparedStatement answerTextStmt = conn.prepareStatement(answerTextQuery)) {
	                    answerTextStmt.setInt(1, answerId);
	                    answerTextStmt.setInt(2, repositoryId);
	                    
	                    try (ResultSet answerResultSet = answerTextStmt.executeQuery()) {
	                        String answerText = "";
	                        if (answerResultSet.next()) {
	                            answerText = answerResultSet.getString("text");
	                        }

	                        // Insert the answer into Exam_Question_Answer
	                        String insertAnswerQuery = "INSERT INTO Exam_Question_Answer (exam_id, question_id, repository_id, answer_id) VALUES (?, ?, ?, ?)";
	                        try (PreparedStatement insertAnswerStmt = conn.prepareStatement(insertAnswerQuery)) {
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
	}


	// Check if the answer is already in the exam
	private boolean isAnswerAlreadyInExam(Connection conn, int examId, int questionId, int answerId, int repositoryId) throws SQLException {
	    // SQL query to check if the answer is already in the exam
	    String query = """
	            SELECT COUNT(*)
	            FROM Exam_Question_Answer
	            WHERE exam_id = ? AND question_id = ? AND answer_id = ? AND repository_id = ?;
	            """;

	    try (PreparedStatement stmt = conn.prepareStatement(query)) {
	        stmt.setInt(1, examId);
	        stmt.setInt(2, questionId);
	        stmt.setInt(3, answerId);
	        stmt.setInt(4, repositoryId);

	        try (ResultSet resultSet = stmt.executeQuery()) {
	            if (resultSet.next()) {
	                // Return true if the count is greater than 0, indicating the answer is in the exam
	                return resultSet.getInt(1) > 0;
	            } else {
	                // If no results are found, return false
	                return false;
	            }
	        }
	    }
	}

}
