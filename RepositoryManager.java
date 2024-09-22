package ItayNetaDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class RepositoryManager {
	private Connection connection;

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public void createNewRepository(String repositoryName) {
        String sql = "INSERT INTO Repository (Name) VALUES (?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, repositoryName);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean repositoryExists(String repositoryName) throws SQLException {
        String sql = "SELECT 1 FROM Repository WHERE name = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, repositoryName);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next();
        }
    }

    public void displayRepositories() {
        String sql = "SELECT * FROM Repository";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int id = rs.getInt("Id");
                String name = rs.getString("Name");
                System.out.println("ID: " + id + ", Name: " + name);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Repository selectRepository(int repositoryId) {
        String sql = "SELECT * FROM Repository WHERE Id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, repositoryId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String name = rs.getString("Name");
                    return new Repository(repositoryId, name, connection);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

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

}
