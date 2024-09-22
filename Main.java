package ItayNetaDatabase;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Scanner;

public class Main {
	static Scanner input = new Scanner(System.in);
	static RepositoryManager repositoryManager = new RepositoryManager();
	static Repository currentRepository;

	public static void main(String[] args) throws SQLException, IOException {
		// Establish SQL connection
		Connection connection = DriverManager.getConnection("jdbc:postgresql:finalProject2", "postgres", "1234");
		repositoryManager.setConnection(connection);

		int choice;
		boolean exit = false;
		do {
		    if (currentRepository == null) {
		        displayMainMenu();
		        choice = getValidatedIntInput();

		        switch (choice) {
		            case 1:
		                createNewRepository();
		                break;
		            case 2:
		                selectRepository();
		                break;
		            case 0:
		                System.out.println("Exiting...");
		                exit = true;
		                break;
		            default:
		                System.out.println("Invalid choice. Please try again.");
		                break;
		        }
		    } else {
		        boolean backToMainMenu = false;
		        do {
		            displayRepositoryMenu();
		            choice = getValidatedIntInput();

		            switch (choice) {
		                case 1:
		                    addQuestion();
		                    break;
		                case 2:
		                    addAnswer();
		                    break;
		                case 3:
		                    addAnswerToQuestion();
		                    break;
		                case 4:
		                    deleteQuestion();
		                    break;
		                case 5:
		                    deleteAnswerFromQuestion();
		                    break;
		                case 6:
		                    displayQuestionsAndAnswers();
		                    break;
		                case 7:
		                    updateQuestion();
		                    break;
		                case 8:
		                    updateOpenQuestionAnswer();
		                    break;
		                case 9:
		                    createAutomaticExam();
		                    break;
		                case 10:
		                    createManualExam();
		                    break;
		                case 0:
		                    currentRepository = null; // Deselect repository
		                    System.out.println("Back to main menu.");
		                    backToMainMenu = true;
		                    break;
		                default:
		                    System.out.println("Invalid choice. Please try again.");
		                    break;
		            }
		        } while (!backToMainMenu);
		    }
		} while (!exit);

		// Close connection when done
		connection.close();
	}

	private static void displayMainMenu() {
		System.out.println("----- Main Menu -----");
		System.out.println("1. Create a new repository");
		System.out.println("2. Select a repository to perform actions on");
		System.out.println("0. Exit");
		System.out.println("---------------------");
	}

	private static void displayRepositoryMenu() {
		System.out.println("----- Repository Menu -----");
		System.out.println("1. Add a question");
		System.out.println("2. Add an answer");
		System.out.println("3. Add an answer to american question");
		System.out.println("4. Delete a question");
		System.out.println("5. Delete an answer from an American question");
		System.out.println("6. Display all questions and answers in the selected repository");
		System.out.println("7. Update a question");
		System.out.println("8. Update the answer of an open question");
		System.out.println("9. Create an automatic exam");
		System.out.println("10. Create a manual exam");
		System.out.println("0. Exit to main menu");
		System.out.println("--------------------------");
	}

	private static void createNewRepository() {
		System.out.println("Please type the name of the repository you want to create: ");
		String repositoryName = input.nextLine();
		repositoryManager.createNewRepository(repositoryName);
	}

	private static void selectRepository() throws SQLException {
		if (repositoryManager.getNumberOfRepositories() == 0) {
			System.out.println("There aren't any repositories");
			return;
		}
		System.out.println("Please select one of the following repository's number: ");
		repositoryManager.displayRepositories();
		System.out.println("Enter 0 to go back to the main menu.");
		int repositoryId = getValidatedIntInput();

		if (repositoryId == 0) {
			return;
		}

		currentRepository = repositoryManager.selectRepository(repositoryId);
		if (currentRepository == null) {
			System.out.println("No repository selected.");
		} else {
			System.out.println("Selected Repository: " + currentRepository.getName());
		}
	}

	private static void addQuestion() throws SQLException {
		if (!isRepositorySelected())
			return;

		System.out.println("Enter question text: ");
		String questionText = input.nextLine();

		System.out.println("Choose question type: 'o' for open or 'a' for American:");
		String questionType = input.nextLine().toLowerCase();
		while (!questionType.equals("o") && !questionType.equals("a")) {
			System.out.println("Invalid input. Please choose 'o' or 'a':");
			questionType = input.nextLine().toLowerCase();
		}

		System.out.println("Enter difficulty (1 = Easy, 2 = Medium, 3 = Hard): ");
		int difficultyId = getValidatedIntInput();
		while (difficultyId < 1 || difficultyId > 3) {
			System.out.println("Invalid difficulty. Please enter 1, 2, or 3:");
			difficultyId = getValidatedIntInput();
		}

		String difficulty = mapDifficultyToString(difficultyId);

		if (questionType.equals("a")) {
			int questionId = currentRepository.addAmericanQuestion(questionText, difficulty);
			handleAmericanQuestionAnswers(questionId);
		} else {
			handleOpenQuestion(questionText, difficulty);
		}
	}

	private static String mapDifficultyToString(int difficultyId) {
		switch (difficultyId) {
		case 1:
			return "easy";
		case 2:
			return "medium";
		case 3:
			return "hard";
		default:
			throw new IllegalArgumentException("Invalid difficulty level.");
		}
	}

	private static void handleAmericanQuestionAnswers(int questionId) throws SQLException {
		System.out.println("Would you like to add answers? (y/n):");
		String addAnswers = input.nextLine().toLowerCase();
		if (addAnswers.equals("y")) {
			System.out.println("How many answers would you like to add? (1-8):");
			int numAnswers = getValidatedIntInput();
			while (numAnswers < 1 || numAnswers > 8) {
				System.out.println("Invalid number. Choose between 1 and 8:");
				numAnswers = getValidatedIntInput();
			}

			for (int i = 0; i < numAnswers; i++) {
				System.out.println("Enter answer text:");
				String answerText = input.nextLine();

				System.out.println("Is this answer correct? (t/f):");
				String isCorrectStr = input.nextLine().toLowerCase();
				boolean isCorrect = isCorrectStr.equals("t");

				int answerId = currentRepository.doesAnswerExist(answerText, currentRepository.getId());
				if (answerId == -1) {
					// Answer does not exist, so add it
					answerId = currentRepository.addAnswer(answerText, currentRepository.getId());
				}

				// Add the answer to the American question
				if (!currentRepository.addAnswerToAmericanQuestion(questionId, answerId, isCorrect))
					i--;
			}
		}
	}

	private static void handleOpenQuestion(String questionText, String difficulty) throws SQLException {
		System.out.println("Enter the answer for the open question:");
		String openAnswerText = input.nextLine();

		int answerId = currentRepository.doesAnswerExist(openAnswerText, currentRepository.getId());
		if (answerId == -1) {
			// Answer does not exist, so add it
			answerId = currentRepository.addAnswer(openAnswerText, currentRepository.getId());
		}

		currentRepository.addOpenQuestion(questionText, difficulty, answerId);
	}

	private static void addAnswer() throws SQLException {
		if (!isRepositorySelected())
			return;

		System.out.println("Enter answer text: ");
		String answerText = input.nextLine();

		int answerId = currentRepository.doesAnswerExist(answerText, currentRepository.getId());
		if (answerId == -1) {
			// Answer does not exist, add it
			answerId = currentRepository.addAnswer(answerText, currentRepository.getId());
		} else {
			System.out.println("The answer already exists in the repository.");
		}
	}

	private static void addAnswerToQuestion() throws SQLException {
		if (!isRepositorySelected())
			return;

		// Display all questions
		System.out.println("Available American questions:");
		currentRepository.displayAmericanQuestions();

		// Prompt user for question ID and validate it
		System.out.println("Enter question ID: ");
		int questionId = getValidatedIntInput();

		// Validate question ID
		if (!currentRepository.questionExists(questionId)) {
			System.out.println("Invalid question ID. Please try again.");
			return;
		}

		// Display all answers
		System.out.println("Available answers:");
		currentRepository.displayAllAnswers();

		// Prompt user for answer ID and validate it
		System.out.println("Enter answer ID: ");
		int answerId = getValidatedIntInput();

		// Validate answer ID
		if (!currentRepository.answerExists(answerId)) {
			System.out.println("Invalid answer ID. Please try again.");
			return;
		}

		System.out.println("Is this answer correct? Type 't' for true or 'f' for false:");
		String isCorrectStr = input.nextLine();

		while (!isCorrectStr.equalsIgnoreCase("t") && !isCorrectStr.equalsIgnoreCase("f")) {
			System.out.println("Invalid input. Please enter 't' or 'f':");
			isCorrectStr = input.nextLine();
		}
		boolean isCorrect = isCorrectStr.equalsIgnoreCase("t");

		// Add answer to question
		currentRepository.addAnswerToAmericanQuestion(questionId, answerId, isCorrect);
	}

	private static void deleteQuestion() throws SQLException {
		if (!isRepositorySelected())
			return;

		// Display all questions
		System.out.println("Available questions:");
		currentRepository.displayAllQuestions();
		System.out.println("Enter question ID to delete: ");
		int questionId = getValidatedIntInput();

		// Validate question ID
		if (!currentRepository.questionExists(questionId)) {
			System.out.println("Invalid question ID. Please try again.");
			return;
		}

		currentRepository.deleteQuestion(questionId);
		System.out.println("Question deleted successfully.");
	}

	private static void deleteAnswerFromQuestion() throws SQLException {
		if (!isRepositorySelected())
			return;

		// Display all questions
		System.out.println("Available questions:");
		currentRepository.displayAllQuestions();

		System.out.println("Enter the ID of the American question from which you want to delete the answer:");
		int questionId = getValidatedIntInput();

		// Validate question ID
		if (!currentRepository.questionExists(questionId)) {
			System.out.println("Invalid question ID. Please try again.");
			return;
		}

		// Check if the question is an American question
		if (!currentRepository.isAmericanQuestion(questionId)) {
			System.out.println(
					"Selected question is not an American question. Only answers from American questions can be deleted.");
			return;
		}

		// Display the selected question with its answers
		currentRepository.displayQuestionWithAnswers(questionId);

		System.out.println("Enter the answer ID to delete:");
		int answerId = getValidatedIntInput();

		// Validate answer ID
		if (!currentRepository.answerExistsForQuestion(questionId, answerId)) {
			System.out.println("Invalid answer ID for the selected question. Please try again.");
			return;
		}

		// Delete the answer from the American question
		currentRepository.deleteAnswerFromAmericanQuestion(questionId, answerId);
		System.out.println("Answer deleted successfully.");
	}

	private static void displayQuestionsAndAnswers() throws SQLException {
		if (!isRepositorySelected())
			return;

		currentRepository.displayQuestionsAndAnswers();
	}

	private static void updateQuestion() throws SQLException {
		if (!isRepositorySelected())
			return;

		// Display all questions
		System.out.println("Available questions:");
		currentRepository.displayAllQuestions();

		System.out.println("Enter question ID to update:");
		int questionId = getValidatedIntInput();

		// Validate question ID
		if (!currentRepository.questionExists(questionId)) {
			System.out.println("Invalid question ID. Please try again.");
			return;
		}

		System.out.println("Enter new question text:");
		String newQuestionText = input.nextLine();

		currentRepository.updateQuestion(questionId, newQuestionText);
		System.out.println("Question updated successfully.");
	}

	private static void updateOpenQuestionAnswer() throws SQLException {
		if (!isRepositorySelected())
			return;

		// Display all open questions
		System.out.println("Available open questions:");
		currentRepository.displayOpenQuestions();

		System.out.println("Enter open question ID to update its answer:");
		int questionId = getValidatedIntInput();

		// Validate question ID
		if (!currentRepository.questionExists(questionId)) {
			System.out.println("Invalid question ID. Please try again.");
			return;
		}

		// Validate that the question is an open question
		if (currentRepository.isAmericanQuestion(questionId)) {
			System.out.println("The selected question is not an open question. Please try again.");
			return;
		}

		System.out.println("Enter new answer text:");
		String newAnswerText = input.nextLine();

		currentRepository.updateOpenQuestionAnswer(questionId, newAnswerText);
		System.out.println("Answer updated successfully.");
	}

	private static void createAutomaticExam() throws SQLException, IOException {
		if (!isRepositorySelected())
			return;

		System.out.println("Enter the number of questions for the automatic exam:");
		int numQuestions = getValidatedIntInput();

		AutomaticExam automaticExam = new AutomaticExam(numQuestions);
		int examId = 0;

		try {
			examId = automaticExam.createExam(currentRepository);
			currentRepository.createExamFile(examId);
			System.out.println("Automatic exam created successfully.");

		} catch (IllegalArgumentException e) {
			System.out.println("Error: " + e.getMessage());
		} catch (SQLException e) {
			System.out.println("Database error: " + e.getMessage());
		} catch (IOException e) {
			// Check if the cause of IOException is IllegalArgumentException
			if (e.getCause() instanceof IllegalArgumentException) {
				System.out.println(e.getCause().getMessage());
			} else {
				System.out.println("IO error: " + e.getMessage());
			}
		} catch (Exception e) {
			System.out.println("An unexpected error occurred: " + e.getMessage());
		}
	}

	private static void createManualExam() throws SQLException, IOException {
		if (!isRepositorySelected())
			return;

		try {
			System.out.println("Enter the number of questions for the manual exam:");
			int numQuestions = getValidatedIntInput();

			ManualExam manualExam = new ManualExam(numQuestions);
			int examId = manualExam.createExam(currentRepository);
			currentRepository.createExamFile(examId);
			System.out.println("Manual exam created successfully.");

		} catch (IllegalArgumentException e) {
			System.out.println("Error: " + e.getMessage());
		} catch (SQLException e) {
			System.out.println("Database error: " + e.getMessage());
		} catch (IOException e) {
			System.out.println("IO error: " + e.getMessage());
		} catch (Exception e) {
			System.out.println("An unexpected error occurred: " + e.getMessage());
		}
	}

	private static int getValidatedIntInput() {
		int result;
		while (true) {
			if (input.hasNextInt()) {
				result = input.nextInt();
				input.nextLine(); // Clean buffer
				break;
			} else {
				System.out.println("Invalid input. Please enter a valid number.");
				input.nextLine(); // Clean buffer
			}
		}
		return result;
	}

	private static boolean isRepositorySelected() {
		if (currentRepository == null) {
			System.out.println("No repository selected.");
			return false;
		}
		return true;
	}

}
