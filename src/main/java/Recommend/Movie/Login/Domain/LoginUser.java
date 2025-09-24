package Recommend.Movie.Login.Domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "users")
public class LoginUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String name;
    private String gender;
    private String birthdate;



    public LoginUser(String email, String name, String gender, String birthdate) {
        this.email = email;
        this.name = name;
        this.gender = gender;
        this.birthdate = birthdate;
    }
}


