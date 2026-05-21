Move all timestamp fields to use @createdDate and @LastModifiedDate

Make sure any code that uses the timestamps is updated, and that the seed script in ./scripts is updated as well as the mock data in the frontend.

"" import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import java.time.Instant;

public class GameSession {

    @CreatedDate
    private Instant createdAt; // Spring automatically sets this on insert

    @LastModifiedDate
    private Instant updatedAt; ""
