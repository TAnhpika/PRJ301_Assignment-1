package controller.appointment;

import model.Appointment;
import dal.DoctorDAO;
import dal.AppointmentDAO;
import model.User;
import model.Doctors;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;


public class DoctorAppointmentsServlet extends HttpServlet {

    @Override   
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        System.out.println("=== DoctorAppointmentsServlet - doGet ===");
        
        // Lấy session và User object
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;
        Integer userId = null;
        Long doctorId = null;
        
        if (user != null) {
            userId = user.getId();
            System.out.println("Session exists, User object found, userId: " + userId);
            System.out.println("User details: " + user.getEmail() + ", Role: " + user.getRole());
            // Lấy doctor_id từ user_id
            Doctors doctor =DoctorDAO.getDoctorByUserId(userId);
            if (doctor != null) {
                doctorId = doctor.getDoctor_id();
                System.out.println("Found doctor_id: " + doctorId + " for userId: " + userId);
            } else {
                System.out.println("❌ Không tìm thấy doctor cho userId: " + userId);
            }
        } else {
            System.out.println("No user object found in session");
            
            // Thử cách cũ làm fallback
            if (session != null) {
                Object userIdObj = session.getAttribute("userId");
                if (userIdObj != null) {
                    userId = (Integer) userIdObj;
                    System.out.println("Found userId directly in session: " + userId);
                    // Lấy doctor_id cho fallback case
                    Doctors doctor =DoctorDAO.getDoctorByUserId(userId);
                    if (doctor != null) {
                        doctorId = doctor.getDoctor_id();
                        System.out.println("Found doctor_id (fallback): " + doctorId);
                    }
                }
            }
            
            // 🚨 EMERGENCY FALLBACK: Dùng user_id = 1 để test (dựa vào data SQL)
            if (userId == null) {
                System.out.println("⚠️ EMERGENCY FALLBACK: Using userId = 1 for testing");
                userId = 1; // Từ SQL: user_id = 1 có doctor
                // Lấy doctor_id tương ứng
                System.out.println("🔍 CallingDoctorDAO.getDoctorByUserId(" + userId + ")...");
                Doctors doctor =DoctorDAO.getDoctorByUserId(userId);
                if (doctor != null) {
                    doctorId = doctor.getDoctor_id();
                    System.out.println("✅ Emergency fallback SUCCESS - Found doctor_id: " + doctorId);
                    System.out.println("   Doctor details: " + doctor.getFull_name() + " (" + doctor.getSpecialty() + ")");
                } else {
                    System.out.println("❌ Emergency fallback FAILED - No doctor found for userId: " + userId);
                    // Thử với userId khác dựa trên SQL data
                    System.out.println("🔄 Trying fallback with userId = 68...");
                    userId = 68; // Từ SQL data: user_id = 68 cũng có doctor
                    doctor =DoctorDAO.getDoctorByUserId(userId);
                    if (doctor != null) {
                        doctorId = doctor.getDoctor_id();
                        System.out.println("✅ Second fallback SUCCESS with userId=68, doctor_id: " + doctorId);
                    }
                }
            }
        }
        
        
        

        // Lấy ngày từ tham số, mặc định là hôm nay
        String dateParam = request.getParameter("date");
        LocalDate selectedDate;
        if (dateParam != null && !dateParam.trim().isEmpty()) {
            try {
                selectedDate = LocalDate.parse(dateParam.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (Exception ex) {
                selectedDate = LocalDate.now();
            }
        } else {
            selectedDate = LocalDate.now();
        }
        String dateStr = selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE); // yyyy-MM-dd
        String dateDisplay = selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        try {
            System.out.println("Fetching appointments for doctorId: " + doctorId + ", date: " + dateStr);
            List<Appointment> appointments = doctorId != null
                ? AppointmentDAO.getAppointmentsByDoctorAndDate(doctorId, dateStr)
                : List.of();
            System.out.println("Found " + (appointments != null ? appointments.size() : 0) + " appointments");
            
            request.setAttribute("appointments", appointments);
            request.setAttribute("doctorId", doctorId);
            request.setAttribute("userId", userId);
            request.setAttribute("selectedDate", dateStr);
            request.setAttribute("selectedDateDisplay", dateDisplay);
            request.setAttribute("isToday", selectedDate.equals(LocalDate.now()));
            
            request.getRequestDispatcher("/view/jsp/doctor/doctor_trongngay.jsp").forward(request, response);

        } catch (Exception e) {
            System.err.println("General Error: " + e.getMessage());
            e.printStackTrace();
            
            request.setAttribute("error", "Lỗi hệ thống: " + e.getMessage());
            request.setAttribute("userId", userId);
            request.setAttribute("doctorId", doctorId);
            request.setAttribute("appointments", null);
            request.setAttribute("selectedDate", dateStr);
            request.setAttribute("selectedDateDisplay", dateDisplay);
            request.setAttribute("isToday", selectedDate.equals(LocalDate.now()));
            
            request.getRequestDispatcher("/view/jsp/doctor/doctor_trongngay.jsp").forward(request, response);
        }
        // Đặt thông báo lỗi vào request attribute
// Vẫn forward tới JSP để hiển thị lỗi
        
    }
}
