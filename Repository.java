package ItayNetaDatabase;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Repository {
	private int id;
	private String name;
	private Connection connection;

	public Repository(int id, String name, Connection connection) {
		this.id = id;
		this.name = name;
		this.connection = connection;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Connection getConnection() {
		return connection;
	}

	public void setConnection(Connection connection) {
		this.connection = connection;
	}

	public int doesAnswerExist(String text, int repositoryId) throws SQLException {
		// Check if the answer exists in the repository
		String sql = "SELECT id FROM Answer WHERE text = ? AND repository_id = ?";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, text);
			statement.setInt(2, repositoryId);
			ResultSet resultSet = statement.executeQuery();

			if (resultSet.next()) {
				// Return the ID of the existing answer
				return resultSet.getInt("id");
			} else {
				// Answer does not exist
				return -1;
			}
		}
	}

	// Add an answer
	public int addAnswer(String text, int repositoryId) throws SQLException {
		// First, check if the answer already exists
		int existingAnswerId = doesAnswerExist(text, repositoryId);
		if (existingAnswerId != -1) {
			// Answer already exists, return -1
			System.out.println("An answer with the same text already exists in this repository.");
			return -1;
		}

		// Proceed to add the answer
		String sql = "INSERT INTO Answer (text, repository_id) VALUES (?, ?)";
		try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
			statement.setString(1, text);
			statement.setInt(2, repositoryId);
			statement.executeUpdate();

			// Retrieve the generated ID
			try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
				if (generatedKeys.next()) {
					return generatedKeys.getInt(1); // Return the generated ID
				} else {
					throw new SQLException("Creating answer failed, no ID obtained.");
				}
			}
		}
	}

	// Add an answer to a question
	public boolean addAnswerToAmericanQuestion(int questionId, int answerId, boolean isCorrect) throws SQLException {
		// Check if the answer already belongs to the question
		String checkSql = "SELECT 1 FROM American_Question_Answer WHERE american_question_id = ? AND answer_id = ? AND repository_id = ?";
		try (PreparedStatement checkStatement = connection.prepareStatement(checkSql)) {
			checkStatement.setInt(1, questionId);
			checkStatement.setInt(2, answerId);
			// Assuming you have a repository ID available when adding the answer
			checkStatement.setInt(3, this.id);
			ResultSet resultSet = checkStatement.executeQuery();

			if (resultSet.next()) {
				System.out.println("This answer is already associated with the selected American question.");
				return false;
			}
		}

		// Insert the answer into the American_Question_Answer table
		String sql = "INSERT INTO American_Question_Answer (american_question_id, answer_id, repository_id, is_correct) VALUES (?, ?, ?, ?)";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, questionId);
			statement.setInt(2, answerId);
			// Add repository ID as it's part of the composite key
			statement.setInt(3, this.id);
			statement.setBoolean(4, isCorrect);
			statement.executeUpdate();
		}
		return true;
	}

	public void updateQuestion(int questionId, String newText) throws SQLException {
		String sql = "UPDATE Question SET text = ? WHERE id = ? AND repository_id = ?";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, newText);
			statement.setInt(2, questionId);
			statement.setInt(3, this.id); // Ensure the question belongs to the current repository
			int rowsUpdated = statement.executeUpdate();

			if (rowsUpdated == 0) {
				throw new SQLException("No matching question found with the provided ID.");
			}
		}
	}

	public void updateOpenQuestionAnswer(int questionId, String newAnswerText) throws SQLException {
		// Get the Answer_id from the Open_Question table
		String getAnswerSql = "SELECT Answer_id FROM Open_Question WHERE Id = ? AND Repository_id = ?";
		try (PreparedStatement getAnswerStatement = connection.prepareStatement(getAnswerSql)) {
			getAnswerStatement.setInt(1, questionId);
			getAnswerStatement.setInt(2, this.id); // Use this.id for the repository ID
			ResultSet resultSet = getAnswerStatement.executeQuery();

			if (resultSet.next()) {
				int answerId = resultSet.getInt("Answer_id");

				// Update the Answer text
				String updateAnswerSql = "UPDATE Answer SET text = ? WHERE id = ? AND repository_id = ?";
				try (PreparedStatement updateAnswerStatement = connection.prepareStatement(updateAnswerSql)) {
					updateAnswerStatement.setString(1, newAnswerText);
					updateAnswerStatement.setInt(2, answerId);
					updateAnswerStatement.setInt(3, this.id); // Use this.id for the repository ID
					updateAnswerStatement.executeUpdate();
				}
			} else {
				System.out.println("No open question found with the specified ID.");
			}
		}
	}

	// Delete a question
	public void deleteQuestion(int questionId) throws SQLException {
		// Delete the question (this will cascade to AmericanQuestion or OpenQuestion
		// due to the foreign key)
		String sql = "DELETE FROM Question WHERE id = ? AND repository_id = ?";
		try (PreparedStatement deleteQuestionStmt = connection.prepareStatement(sql)) {
			deleteQuestionStmt.setInt(1, questionId);
			deleteQuestionStmt.setInt(2, this.id);
			deleteQuestionStmt.executeUpdate();
		}
		// No need to manually delete from AmericanQuestion_Answer; the trigger will
		// handle it
	}

	// Delete an answer
	public void deleteAnswer(int answerId) throws SQLException {
		String sql = "DELETE FROM Answer WHERE id = ? AND repository_id = ?";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, answerId);
			statement.setInt(2, this.id);
			statement.executeUpdate();
		}
	}

	public void deleteAnswerFromAmericanQuestion(int questionId, int answerId) throws SQLException {
		// Delete the answer from American_Question_Answer table
		String sql = "DELETE FROM American_Question_Answer WHERE American_question_id = ? AND Answer_id = ? AND Repository_id = ?";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, questionId);
			statement.setInt(2, answerId);
			statement.setInt(3, this.id);
			statement.executeUpdate();
		}
	}

	public void displayQuestionWithAnswers(int questionId) throws SQLException {
		// SQL query to select question details and associated answers
		String sql = "SELECT q.text AS question_text, q.difficulty AS question_difficulty, "
				+ "a.id AS answer_id, a.text AS answer_text, aq.is_correct AS is_correct " + "FROM Question q "
				+ "LEFT JOIN American_Question_Answer aq ON q.Id = aq.American_question_id AND q.Repository_id = aq.Repository_id "
				+ "LEFT JOIN Answer a ON aq.Answer_id = a.Id AND a.Repository_id = q.Repository_id "
				+ "WHERE q.Id = ? AND q.Repository_id = ?";

		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, questionId);
			statement.setInt(2, this.id); // Use this.id for the repository ID
			try (ResultSet rs = statement.executeQuery()) {
				if (rs.next()) {
					// Display the question and difficulty
					String questionText = rs.getString("question_text");
					String questionDifficulty = rs.getString("question_difficulty");
					System.out.println(questionText + " [" + questionDifficulty.toUpperCase() + "]");

					// Loop through the answers
					do {
						int answerId = rs.getInt("answer_id");
						String answerText = rs.getString("answer_text");
						boolean isCorrect = rs.getBoolean("is_correct");

						// Display each answer with its ID and correctness
						System.out.println("ID: " + answerId + ". " + answerText + " (" + isCorrect + ")");
					} while (rs.next());
				} else {
					System.out.println("No answers found for the selected question.");
				}
			}
		}
	}

	// Display all questions and answers
	public void displayQuestionsAndAnswers() throws SQLException {
		String sql = "SELECT q.id AS question_id, q.text AS question_text, q.difficulty AS question_difficulty, "
				+ "q.type AS question_type, COALESCE(aqa.answer_id, 0) AS american_answer_id, "
				+ "ans1.text AS american_answer_text, aqa.is_correct AS is_correct, "
				+ "oq.answer_id AS open_answer_id, ans2.text AS open_answer_text " + "FROM Question q "
				+ "LEFT JOIN American_Question_Answer aqa ON q.id = aqa.american_question_id AND q.repository_id = aqa.repository_id "
				+ "LEFT JOIN Answer ans1 ON aqa.answer_id = ans1.id AND ans1.repository_id = q.repository_id "
				+ "LEFT JOIN Open_Question oq ON q.id = oq.id AND q.repository_id = oq.repository_id "
				+ "LEFT JOIN Answer ans2 ON oq.answer_id = ans2.id AND ans2.repository_id = q.repository_id "
				+ "WHERE q.repository_id = ?";

		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, id); // repository id
			ResultSet resultSet = statement.executeQuery();

			int previousQuestionId = -1;
			int answerCounter = 1; // For numbering American question answers

			while (resultSet.next()) {
				int currentQuestionId = resultSet.getInt("question_id");
				String questionText = resultSet.getString("question_text");
				String questionDifficulty = resultSet.getString("question_difficulty"); // Difficulty enum
				String questionType = resultSet.getString("question_type"); // Question type (open/american)
				int americanAnswerId = resultSet.getInt("american_answer_id");
				String americanAnswerText = resultSet.getString("american_answer_text");
				boolean isCorrect = resultSet.getBoolean("is_correct");
				int openAnswerId = resultSet.getInt("open_answer_id");
				String openAnswerText = resultSet.getString("open_answer_text");

				// Print the question and its difficulty when it's a new question
				if (currentQuestionId != previousQuestionId) {
					if (previousQuestionId != -1) {
						System.out.println(); // Add a newline between questions
					}
					System.out.println(questionText + " [" + questionDifficulty.toUpperCase() + "]");
					previousQuestionId = currentQuestionId;
					answerCounter = 1; // Reset answer counter for each new question
				}

				// Print American question answers in the desired format
				if (americanAnswerId != 0 && "american".equals(questionType)) {
					System.out.println(answerCounter + ". " + americanAnswerText + " (" + isCorrect + ")");
					answerCounter++;
				}

				// Print Open question answer in the desired format
				if (openAnswerId != 0 && "open".equals(questionType)) {
					System.out.println("Answer: " + openAnswerText);
				}
			}
		}
	}

	public void displayOpenQuestions() throws SQLException {
		// SQL query to fetch Open questions from the Question table joined with
		// Open_Question
		String sql = "SELECT q.id, q.text FROM Question q " + "JOIN Open_Question o ON q.id = o.id "
				+ "WHERE q.repository_id = ?";

		// Use try-with-resources to ensure the PreparedStatement and ResultSet are
		// closed properly
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			// Set the repository ID parameter
			statement.setInt(1, this.id); // Assuming `this.id` is the repository ID

			// Execute the query
			try (ResultSet resultSet = statement.executeQuery()) {
				// Process the result set
				while (resultSet.next()) {
					int questionId = resultSet.getInt("id");
					String questionText = resultSet.getString("text");
					// Display the question ID and text
					System.out.println("ID: " + questionId + ". Text: " + questionText);
				}
			}
		}
	}

	public void displayAllQuestions() throws SQLException {
		String sql = "SELECT id, text FROM Question WHERE repository_id = ?";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, this.id); // Assuming 'id' is the repository ID
			try (ResultSet rs = statement.executeQuery()) {
				while (rs.next()) {
					int questionId = rs.getInt("Id");
					String questionText = rs.getString("Text");
					System.out.println("Question ID: " + questionId + ". Text: " + questionText);
				}
			}
		}
	}

	public void displayAmericanQuestions() throws SQLException {
		String sql = "SELECT id, text, difficulty FROM Question WHERE type = 'american' AND repository_id = ?";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, this.id); // repository ID
			try (ResultSet rs = statement.executeQuery()) {
				while (rs.next()) {
					int questionId = rs.getInt("id");
					String questionText = rs.getString("text");
					String difficulty = rs.getString("difficulty");
					System.out
							.println("ID: " + questionId + ". " + questionText + " [" + difficulty.toUpperCase() + "]");
				}
			}
		}
	}

	public void displayAllAnswers() throws SQLException {
		// SQL query to fetch all answers from the Answer table for a specific
		// repository
		String sql = "SELECT id, text FROM Answer WHERE repository_id = ?";

		// Use try-with-resources to ensure PreparedStatement and ResultSet are closed
		// properly
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			// Set the repository ID parameter
			statement.setInt(1, this.id); // Assuming `this.id` is the repository ID

			// Execute the query
			try (ResultSet rs = statement.executeQuery()) {
				// Process the result set
				while (rs.next()) {
					int answerId = rs.getInt("id");
					String answerText = rs.getString("text");
					// Display the answer ID and text
					System.out.println("Answer ID: " + answerId + ". Text: " + answerText);
				}
			}
		}
	}

	// Additional methods for managing American and Open questions
	// Add an American question
	public int addAmericanQuestion(String text, String difficulty) throws SQLException {
		// SQL to insert a new question
		String sql = "INSERT INTO Question (text, difficulty, type, repository_id) VALUES (?, ?::difficulty_enum, 'american', ?)";

		try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
			// Set parameters for the Question insertion
			statement.setString(1, text);
			statement.setString(2, difficulty); // Difficulty as ENUM
			statement.setInt(3, this.id);

			// Execute the update
			statement.executeUpdate();

			// Retrieve the generated question ID
			try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
				if (generatedKeys.next()) {
					int questionId = generatedKeys.getInt(1);

					// Insert into American_Question table
					String americanSql = "INSERT INTO American_Question (Id, Repository_id) VALUES (?, ?)";
					try (PreparedStatement americanStatement = connection.prepareStatement(americanSql)) {
						americanStatement.setInt(1, questionId);
						americanStatement.setInt(2, this.id); // Ensure Repository_id is set
						americanStatement.executeUpdate();
					}

					return questionId;
				} else {
					throw new SQLException("Creating American question failed, no ID obtained.");
				}
			}
		}
	}

	// Add an Open question
	public int addOpenQuestion(String text, String difficulty, int answerId) throws SQLException {
		// SQL to insert a new question
		String insertQuestionSql = "INSERT INTO Question (text, difficulty, type, repository_id) VALUES (?, ?::difficulty_enum, 'open', ?)";

		try (PreparedStatement questionStatement = connection.prepareStatement(insertQuestionSql,
				Statement.RETURN_GENERATED_KEYS)) {
			// Set parameters for inserting into Question
			questionStatement.setString(1, text);
			questionStatement.setString(2, difficulty); // Difficulty as ENUM
			questionStatement.setInt(3, this.id); // Repository ID

			// Execute the insertion
			questionStatement.executeUpdate();

			// Retrieve the generated question ID
			try (ResultSet generatedKeys = questionStatement.getGeneratedKeys()) {
				if (generatedKeys.next()) {
					int questionId = generatedKeys.getInt(1);

					// SQL to insert into Open_Question
					String insertOpenQuestionSql = "INSERT INTO Open_Question (Id, Repository_id, Answer_id) VALUES (?, ?, ?)";
					try (PreparedStatement openQuestionStatement = connection.prepareStatement(insertOpenQuestionSql)) {
						openQuestionStatement.setInt(1, questionId);
						openQuestionStatement.setInt(2, this.id); // Repository ID
						openQuestionStatement.setInt(3, answerId); // Answer ID
						openQuestionStatement.executeUpdate();
					}

					return questionId;
				} else {
					throw new SQLException("Creating Open question failed, no ID obtained.");
				}
			}
		}
	}

	/*
	 * public void deleteOpenQuestion(int questionId) throws SQLException { //
	 * Define the SQL statement with both Id and Repository_id in the WHERE clause
	 * String sql = "DELETE FROM Open_Question WHERE Id = ? AND Repository_id = ?";
	 * 
	 * try (PreparedStatement statement = connection.prepareStatement(sql)) { // Set
	 * the parameters for the SQL statement statement.setInt(1, questionId);
	 * statement.setInt(2, this.id); // Assuming this.id is the repository ID
	 * 
	 * // Execute the deletion int rowsAffected = statement.executeUpdate();
	 * 
	 * if (rowsAffected == 0) { System.out.
	 * println("No Open Question found with the specified ID in the repository."); }
	 * }
	 */
	// }

	// Check if repository exists
	public boolean repositoryExists(String repositoryName) throws SQLException {
		String sql = "SELECT 1 FROM Repository WHERE name = ?";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, repositoryName);
			ResultSet resultSet = statement.executeQuery();
			return resultSet.next();
		}
	}

	public boolean questionExists(int questionId) throws SQLException {
		String sql = "SELECT COUNT(*) FROM Question WHERE Id = ? AND Repository_id = ?";

		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, questionId);
			statement.setInt(2, this.id);

			try (ResultSet rs = statement.executeQuery()) {
				// Check if any rows were returned
				if (rs.next()) {
					return rs.getInt(1) > 0;
				}
			}

		}
		return false;
	}

	public boolean answerExists(int answerId) throws SQLException {
		String sql = "SELECT COUNT(*) FROM Answer WHERE id = ? AND repository_id = ?";

		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, answerId);
			statement.setInt(2, this.id);

			try (ResultSet rs = statement.executeQuery()) {
				// Check if any rows were returned
				if (rs.next()) {
					return rs.getInt(1) > 0;
				}
			}
		}
		return false;
	}

	public boolean answerExistsForQuestion(int questionId, int answerId) throws SQLException {
		String sql = "SELECT 1 FROM American_Question_Answer WHERE american_question_id = ? AND answer_id = ? AND repository_id = ?";

		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, questionId);
			statement.setInt(2, answerId);
			statement.setInt(3, this.id); // Assuming this.id is the repository ID

			try (ResultSet rs = statement.executeQuery()) {
				return rs.next();
			}
		}
	}

	public boolean isAmericanQuestion(int questionId) throws SQLException {
		String sql = "SELECT type FROM Question WHERE id = ? AND repository_id = ?";

		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, questionId);
			statement.setInt(2, this.id); // Assuming this.id is the repository ID

			try (ResultSet resultSet = statement.executeQuery()) {
				if (resultSet.next()) {
					String questionType = resultSet.getString("type");
					return "american".equalsIgnoreCase(questionType);
				}
			}
		}
		return false;
	}

	// Get the number of repositories
	public int getNumberOfRepositories() throws SQLException {
		String sql = "SELECT COUNT(*) FROM Repository";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			ResultSet resultSet = statement.executeQuery();
			if (resultSet.next()) {
				return resultSet.getInt(1);
			}
		}
		return 0;
	}

	public void createExamFile(int examId) throws IOException, SQLException {
	    LocalDateTime currentDateTime = LocalDateTime.now(); // Current time
	    DateTimeFormatter fileNameTimeFormat = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm"); // File name pattern
	    DateTimeFormatter examTimeFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm:ss a"); // Exam time format

	    String testFileName = "exam_" + currentDateTime.format(fileNameTimeFormat) + ".txt"; // Exam file
	    String solutionFileName = "solution_" + currentDateTime.format(fileNameTimeFormat) + ".txt"; // Solution file

	    try (FileWriter fwExam = new FileWriter(testFileName);
	         FileWriter fwExamSolution = new FileWriter(solutionFileName)) {

	        fwExam.write("This exam was created at: " + currentDateTime.format(examTimeFormat) + "\n");
	        fwExamSolution.write("This exam was created at: " + currentDateTime.format(examTimeFormat) + "\n");

	        // Query to fetch all questions and their type from the exam
	        String questionQuery = """
	                SELECT q.id AS question_id, q.text AS question_text, q.type AS question_type, eq.repository_id
	                FROM Exam_Question eq
	                JOIN Question q ON eq.question_id = q.id AND eq.repository_id = q.repository_id
	                WHERE eq.exam_id = ?
	                ORDER BY eq.position ASC;
	                """;

	        try (PreparedStatement questionStmt = connection.prepareStatement(questionQuery)) {
	            questionStmt.setInt(1, examId);
	            try (ResultSet questionResult = questionStmt.executeQuery()) {

	                int questionIndex = 1; // For numbering questions

	                while (questionResult.next()) {
	                    int questionId = questionResult.getInt("question_id");
	                    int repositoryId = questionResult.getInt("repository_id"); // Fetch repositoryId
	                    String questionText = questionResult.getString("question_text");
	                    String questionType = questionResult.getString("question_type");

	                    // Write question to exam file
	                    fwExam.write("\n" + questionIndex + ") " + questionText + "\n");

	                    // Write question to solution file
	                    fwExamSolution.write("\n" + questionIndex + ") " + questionText + "\n");

	                    if ("american".equalsIgnoreCase(questionType)) {
	                        // Query to fetch the answers for the American question
	                        String americanAnswersQuery = """
	                                SELECT a.id AS answer_id, a.text AS answer_text, aqa.is_correct
	                                FROM Exam_Question_Answer eqa
	                                JOIN American_Question_Answer aqa 
	                                    ON eqa.question_id = aqa.american_question_id 
	                                    AND eqa.repository_id = aqa.repository_id
	                                    AND eqa.answer_id = aqa.answer_id
	                                JOIN Answer a 
	                                    ON aqa.answer_id = a.id 
	                                    AND aqa.repository_id = a.repository_id
	                                WHERE eqa.exam_id = ? 
	                                  AND eqa.question_id = ? 
	                                  AND eqa.repository_id = ?;
	                                """;

	                        try (PreparedStatement answerStmt = connection.prepareStatement(americanAnswersQuery)) {
	                            answerStmt.setInt(1, examId);
	                            answerStmt.setInt(2, questionId);
	                            answerStmt.setInt(3, repositoryId);
	                            try (ResultSet answerResult = answerStmt.executeQuery()) {

	                                int answerIndex = 1;
	                                while (answerResult.next()) {
	                                    String answerText = answerResult.getString("answer_text");
	                                    boolean isCorrect = answerResult.getBoolean("is_correct");

	                                    // Write answer to exam file
	                                    fwExam.write("\t" + answerIndex + ". " + answerText + "\n");

	                                    // Write answer to solution file with is_correct value
	                                    fwExamSolution.write(
	                                            "\t" + answerIndex + ". " + answerText + " (" + isCorrect + ")\n");

	                                    answerIndex++;
	                                }
	                            }
	                        }

	                    } else if ("open".equalsIgnoreCase(questionType)) {
	                        // Query to fetch the answer for the Open question
	                        String openAnswerQuery = """
	                                SELECT a.text AS answer_text
	                                FROM Open_Question oq
	                                JOIN Answer a ON oq.answer_id = a.id AND oq.repository_id = a.repository_id
	                                WHERE oq.id = ? AND oq.repository_id = ?;
	                                """;

	                        try (PreparedStatement openAnswerStmt = connection.prepareStatement(openAnswerQuery)) {
	                            openAnswerStmt.setInt(1, questionId);
	                            openAnswerStmt.setInt(2, repositoryId);
	                            try (ResultSet openAnswerResult = openAnswerStmt.executeQuery()) {

	                                if (openAnswerResult.next()) {
	                                    String openAnswerText = openAnswerResult.getString("answer_text");

	                                    // Write open question answer to solution file
	                                    fwExam.write("\n");

	                                    // Write correct answer to solution file
	                                    fwExamSolution.write("\t" + openAnswerText + "\n");
	                                }
	                            }
	                        }
	                    }

	                    questionIndex++; // Increment question index for next question
	                }
	            }
	        }

	        System.out.println("Exam and solution files created successfully!");

	    } catch (IOException e) {
	        e.printStackTrace();
	        throw new IOException("Error creating exam files.", e);
	    } catch (SQLException e) {
	        e.printStackTrace();
	        throw new SQLException("Error fetching data from the database.", e);
	    }
	}

}
