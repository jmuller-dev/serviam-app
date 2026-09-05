package com.serviam.app.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.serviam.app.data.model.Chico;
import java.util.List;

@Dao
public interface ChicoDao {

    @Query("SELECT * FROM chicos WHERE agrupacion = :agrupacion AND activo = 1 ORDER BY nombre ASC")
    LiveData<List<Chico>> getActiveChicos(String agrupacion);

    @Query("SELECT * FROM chicos WHERE agrupacion = :agrupacion AND brigada = :brigada AND activo = 1 ORDER BY nombre ASC")
    LiveData<List<Chico>> getChicosByBrigada(String agrupacion, String brigada);

    @Query("SELECT * FROM chicos WHERE dni = :dni LIMIT 1")
    LiveData<Chico> getChicoByDni(String dni);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertChico(Chico chico);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Chico> chicos);

    @Update
    void updateChico(Chico chico);

    @Query("UPDATE chicos SET activo = 0 WHERE dni = :dni")
    void deactivateChico(String dni);
}
